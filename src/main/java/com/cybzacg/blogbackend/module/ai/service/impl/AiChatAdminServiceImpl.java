package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatSession;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelConfigRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiChatSessionRepository;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.model.admin.AiSessionAdminVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiSessionPageQuery;
import com.cybzacg.blogbackend.module.ai.service.AiChatAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 会话后台管理服务实现。
 *
 * <p>提供管理员按条件查询用户 AI 会话、填充用户和渠道信息等能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatAdminServiceImpl implements AiChatAdminService {

    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChannelConfigRepository aiChannelConfigRepository;
    private final SysUserRepository sysUserRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<AiSessionAdminVO> pageSessions(AiSessionPageQuery query) {
        query.setCurrent(PaginationUtils.normalizeCurrent(query.getCurrent()));
        query.setSize(PaginationUtils.normalizeSize(query.getSize(), 10L, 100L));

        Page<AiChatSession> page = aiChatSessionRepository.pageByAdminConditions(query);
        List<AiSessionAdminVO> records = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();

        // 批量填充用户名和渠道名
        fillUserInfo(records);
        fillChannelInfo(records);

        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AiSessionAdminVO getSessionDetail(Long sessionId) {
        AiChatSession session = aiChatSessionRepository.getById(sessionId);
        ExceptionThrowerCore.throwBusinessIfNull(session, ResultErrorCode.AI_SESSION_NOT_FOUND);

        AiSessionAdminVO vo = toAdminVO(session);
        fillUserInfo(List.of(vo));
        fillChannelInfo(List.of(vo));
        log.debug("管理员查询 AI 会话详情: sessionId={}", sessionId);
        return vo;
    }

    /**
     * 将会话实体转换为后台管理 VO，填充基础字段。
     *
     * @param session 会话实体
     * @return 后台管理 VO
     */
    private AiSessionAdminVO toAdminVO(AiChatSession session) {
        AiSessionAdminVO vo = new AiSessionAdminVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setChannelConfigId(session.getChannelConfigId());
        vo.setTitle(session.getTitle());
        vo.setSceneType(session.getSceneType());
        vo.setStatus(session.getStatus());
        vo.setLastMessageAt(session.getLastMessageAt());
        vo.setCreatedAt(session.getCreatedAt());
        vo.setUpdatedAt(session.getUpdatedAt());
        return vo;
    }

    /**
     * 批量填充用户名和昵称，避免 N+1 查询。
     *
     * @param records 会话 VO 列表
     */
    private void fillUserInfo(List<AiSessionAdminVO> records) {
        Set<Long> userIds = records.stream()
                .map(AiSessionAdminVO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return;

        Map<Long, SysUser> userMap = sysUserRepository.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        for (AiSessionAdminVO vo : records) {
            SysUser user = userMap.get(vo.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }
        }
    }

    /**
     * 批量填充渠道名称，避免 N+1 查询。
     *
     * @param records 会话 VO 列表
     */
    private void fillChannelInfo(List<AiSessionAdminVO> records) {
        Set<Long> channelIds = records.stream()
                .map(AiSessionAdminVO::getChannelConfigId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (channelIds.isEmpty()) return;

        Map<Long, AiChannelConfig> channelMap = aiChannelConfigRepository.listByIds(channelIds).stream()
                .collect(Collectors.toMap(AiChannelConfig::getId, Function.identity()));

        for (AiSessionAdminVO vo : records) {
            AiChannelConfig config = channelMap.get(vo.getChannelConfigId());
            if (config != null) {
                vo.setChannelName(config.getChannelName());
            }
        }
    }
}
