package com.cybzacg.blogbackend.module.chat.member.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.chat.ChatChannelCreateApplication;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.chat.ChatChannelApplicationStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.experience.service.UserExperienceService;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatChannelApplicationPageQuery;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatChannelApplicationSubmitRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatChannelApplicationVO;
import com.cybzacg.blogbackend.module.chat.member.repository.ChatChannelCreateApplicationRepository;
import com.cybzacg.blogbackend.module.chat.member.service.UserChatChannelApplicationService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 用户侧频道创建申请服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserChatChannelApplicationServiceImpl implements UserChatChannelApplicationService {
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final ChatChannelCreateApplicationRepository chatChannelCreateApplicationRepository;
    private final SysUserRepository sysUserRepository;
    private final UserExperienceService userExperienceService;
    private final SysConfigService sysConfigService;
    private final ChatModelMapper chatModelMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatChannelApplicationVO submitApplication(ChatChannelApplicationSubmitRequest request) {
        Long userId = SecurityUtils.requireUserId();
        requireActiveUser(userId);
        validateCreateChannelApplicationPermission(userId);
        ChatChannelCreateApplication latest = chatChannelCreateApplicationRepository.findLatestByApplicantUserId(userId);
        if (latest != null && Objects.equals(latest.getApplyStatus(), ChatChannelApplicationStatusEnum.PENDING.getValue())) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "当前已有待审核频道申请，请勿重复提交");
        }
        if (latest != null && Objects.equals(latest.getApplyStatus(), ChatChannelApplicationStatusEnum.NEED_MORE_INFO.getValue())) {
            return resubmitLatestApplication(latest, request);
        }
        LocalDateTime now = LocalDateTime.now();
        ChatChannelCreateApplication application = chatModelMapper.toChannelApplication(request);
        application.setApplicantUserId(userId);
        application.setDesiredSceneType(normalizeDesiredSceneType(application.getDesiredSceneType()));
        application.setApplyStatus(ChatChannelApplicationStatusEnum.PENDING.getValue());
        application.setSubmittedAt(now);
        chatChannelCreateApplicationRepository.save(application);
        return toUserVO(application);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatChannelApplicationVO getLatestApplication() {
        Long userId = SecurityUtils.requireUserId();
        ChatChannelCreateApplication application = chatChannelCreateApplicationRepository.findLatestByApplicantUserId(userId);
        return application == null ? null : toUserVO(application);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ChatChannelApplicationVO> pageMyApplications(ChatChannelApplicationPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        long current = PaginationUtils.normalizeCurrent(query == null ? null : query.getCurrent());
        long size = PaginationUtils.normalizeSize(query == null ? null : query.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        Page<ChatChannelCreateApplication> page = chatChannelCreateApplicationRepository.pageByApplicantUserId(userId, current, size);
        List<ChatChannelApplicationVO> records = page.getRecords().stream().map(this::toUserVO).toList();
        return PageResult.of(page, records);
    }

    private ChatChannelApplicationVO resubmitLatestApplication(ChatChannelCreateApplication latest,
                                                              ChatChannelApplicationSubmitRequest request) {
        ChatChannelCreateApplication updated = chatModelMapper.toChannelApplication(request);
        latest.setDesiredName(updated.getDesiredName());
        latest.setDesiredSceneType(normalizeDesiredSceneType(updated.getDesiredSceneType()));
        latest.setDesiredCategoryCode(updated.getDesiredCategoryCode());
        latest.setDescription(updated.getDescription());
        latest.setApplyStatus(ChatChannelApplicationStatusEnum.PENDING.getValue());
        latest.setReviewerId(null);
        latest.setReviewComment(null);
        latest.setReviewedAt(null);
        latest.setSubmittedAt(LocalDateTime.now());
        chatChannelCreateApplicationRepository.updateById(latest);
        return toUserVO(latest);
    }

    private void requireActiveUser(Long userId) {
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(user == null || Integer.valueOf(1).equals(user.getDeletedFlag()),
                ResultErrorCode.USER_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(user.getStatus(), 1),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前账号不可提交频道申请");
    }

    private void validateCreateChannelApplicationPermission(Long userId) {
        int requiredLevel = getConfigInt(
                ConfigConstants.CHAT_CHANNEL_CREATE_APPLICATION_MIN_LEVEL_KEY,
                ConfigConstants.DEFAULT_CHAT_CHANNEL_CREATE_APPLICATION_MIN_LEVEL
        );
        if (requiredLevel <= 1) {
            return;
        }
        ExceptionThrowerCore.throwBusinessIf(
                !userExperienceService.checkLevelPermission(userId, requiredLevel),
                ResultErrorCode.FORBIDDEN,
                "当前等级不足，至少达到 Lv." + requiredLevel + " 才能申请创建频道"
        );
    }

    private String normalizeDesiredSceneType(String sceneType) {
        String normalized = StrUtils.trimToNull(sceneType);
        if (normalized == null) {
            return ChatConstants.SCENE_TYPE_TOPIC_CHANNEL;
        }
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(normalized, ChatConstants.SCENE_TYPE_TOPIC_CHANNEL),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前仅支持申请主题频道");
        return normalized;
    }

    private int getConfigInt(String key, int defaultValue) {
        String value = sysConfigService.getValueOrDefault(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private ChatChannelApplicationVO toUserVO(ChatChannelCreateApplication application) {
        ChatChannelApplicationVO vo = chatModelMapper.toChannelApplicationVO(application);
        ChatChannelApplicationStatusEnum status = ChatChannelApplicationStatusEnum.fromValue(application.getApplyStatus());
        vo.setApplyStatusLabel(status == null ? null : status.getLabel());
        return vo;
    }
}
