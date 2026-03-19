package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.SysNoticeModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysNoticeAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysNoticeSaveRequest;
import com.cybzacg.blogbackend.module.auth.service.SysNoticeAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysNoticeService;
import com.cybzacg.blogbackend.module.auth.service.SysUserNoticeService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 系统通知后台管理服务实现。
 *
 * <p>负责通知草稿维护、发布撤回，以及指定用户通知的投递关系生成。
 */
@Service
@RequiredArgsConstructor
public class SysNoticeAdminServiceImpl implements SysNoticeAdminService {
    private final SysNoticeService sysNoticeService;
    private final SysUserNoticeService sysUserNoticeService;
    private final SysUserService sysUserService;
    private final SysNoticeModelMapper sysNoticeModelMapper;

    @Override
    public PageResult<SysNoticeAdminVO> pageNotices(SysNoticePageQuery query) {
        Page<SysNotice> page = sysNoticeService.lambdaQuery()
                .like(StringUtils.hasText(query.getTitle()), SysNotice::getTitle, query.getTitle())
                .eq(query.getPublishStatus() != null, SysNotice::getPublishStatus, query.getPublishStatus())
                .eq(query.getTargetType() != null, SysNotice::getTargetType, query.getTargetType())
                .eq(SysNotice::getIsDeleted, 0)
                .orderByDesc(SysNotice::getCreateTime)
                .orderByDesc(SysNotice::getId)
                .page(new Page<>(query.getCurrent(), query.getSize()));

        List<SysNoticeAdminVO> records = page.getRecords().stream()
                .map(notice -> sysNoticeModelMapper.toNoticeAdminVO(notice, sysNoticeModelMapper.toIdList(notice.getTargetUserIds())))
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public SysNoticeAdminVO getNotice(Long id) {
        SysNotice notice = getAvailableNotice(id);
        return sysNoticeModelMapper.toNoticeAdminVO(notice, sysNoticeModelMapper.toIdList(notice.getTargetUserIds()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysNoticeAdminVO createNotice(SysNoticeSaveRequest request) {
        List<Long> targetUserIds = validateTargetUsers(request);
        SysNotice notice = new SysNotice();
        applyFields(notice, request, targetUserIds);
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_DRAFT);
        notice.setIsDeleted(0);
        sysNoticeService.save(notice);
        return sysNoticeModelMapper.toNoticeAdminVO(notice, targetUserIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysNoticeAdminVO updateNotice(Long id, SysNoticeSaveRequest request) {
        SysNotice notice = getEditableNotice(id);
        List<Long> targetUserIds = validateTargetUsers(request);
        applyFields(notice, request, targetUserIds);
        sysNoticeService.updateById(notice);
        return sysNoticeModelMapper.toNoticeAdminVO(notice, targetUserIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNotice(Long id) {
        SysNotice notice = getAvailableNotice(id);
        if (!Objects.equals(NoticeConstants.PUBLISH_STATUS_DRAFT, notice.getPublishStatus())) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "仅未发布通知允许发布");
        }

        Date now = new Date();
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_PUBLISHED);
        notice.setPublishTime(now);
        notice.setRevokeTime(null);
        notice.setPublisherId(SecurityUtils.getUserId());
        sysNoticeService.updateById(notice);
        deliverNotice(notice, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeNotice(Long id) {
        SysNotice notice = getAvailableNotice(id);
        if (!Objects.equals(NoticeConstants.PUBLISH_STATUS_PUBLISHED, notice.getPublishStatus())) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "仅已发布通知允许撤回");
        }
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_REVOKED);
        notice.setRevokeTime(new Date());
        sysNoticeService.updateById(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        SysNotice notice = getAvailableNotice(id);
        notice.setIsDeleted(1);
        sysNoticeService.updateById(notice);
    }

    /**
     * 按目标用户生成通知投递关系，确保指定用户通知在发布后可进入收件箱。
     */
    private void deliverNotice(SysNotice notice, Date now) {
        sysUserNoticeService.lambdaUpdate()
                .eq(SysUserNotice::getNoticeId, notice.getId())
                .remove();
        if (!Objects.equals(NoticeConstants.TARGET_SPECIFIED, notice.getTargetType())) {
            return;
        }
        List<Long> targetUserIds = sysNoticeModelMapper.toIdList(notice.getTargetUserIds());
        if (targetUserIds.isEmpty()) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "指定用户通知缺少目标用户");
        }
        List<SysUserNotice> records = targetUserIds.stream()
                .map(userId -> {
                    SysUserNotice userNotice = new SysUserNotice();
                    userNotice.setNoticeId(notice.getId());
                    userNotice.setUserId(userId);
                    userNotice.setIsRead(NoticeConstants.READ_UNREAD);
                    userNotice.setReadTime(null);
                    userNotice.setCreateTime(now);
                    userNotice.setUpdateTime(now);
                    userNotice.setIsDeleted(0);
                    return userNotice;
                })
                .toList();
        sysUserNoticeService.saveBatch(records);
    }

    /**
     * 将通知请求字段统一回填到实体，并处理目标用户 ID 的持久化格式。
     */
    private void applyFields(SysNotice notice, SysNoticeSaveRequest request, List<Long> targetUserIds) {
        notice.setTitle(normalize(request.getTitle()));
        notice.setContent(request.getContent());
        notice.setType(request.getType());
        notice.setLevel(normalize(request.getLevel()));
        notice.setTargetType(request.getTargetType());
        notice.setTargetUserIds(targetUserIds.isEmpty()
                ? null
                : targetUserIds.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse(null));
    }

    /**
     * 校验通知目标用户范围是否合法，并返回去重后的目标用户列表。
     */
    private List<Long> validateTargetUsers(SysNoticeSaveRequest request) {
        Integer targetType = request.getTargetType();
        if (!Objects.equals(NoticeConstants.TARGET_ALL, targetType)
                && !Objects.equals(NoticeConstants.TARGET_SPECIFIED, targetType)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "通知目标类型非法");
        }
        if (Objects.equals(NoticeConstants.TARGET_ALL, targetType)) {
            return List.of();
        }
        if (request.getTargetUserIds() == null || request.getTargetUserIds().isEmpty()) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "指定用户通知至少选择一个目标用户");
        }
        if (request.getTargetUserIds().stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "目标用户ID不能为空");
        }
        List<Long> distinctUserIds = request.getTargetUserIds().stream().distinct().toList();
        long count = sysUserService.lambdaQuery()
                .in(SysUser::getId, distinctUserIds)
                .eq(SysUser::getDeletedFlag, 0)
                .count();
        if (count != distinctUserIds.size()) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "存在无效目标用户");
        }
        return distinctUserIds;
    }

    /**
     * 获取允许编辑的通知，仅草稿状态通知可修改。
     */
    private SysNotice getEditableNotice(Long id) {
        SysNotice notice = getAvailableNotice(id);
        if (!Objects.equals(NoticeConstants.PUBLISH_STATUS_DRAFT, notice.getPublishStatus())) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "已发布或已撤回通知不允许修改");
        }
        return notice;
    }

    /**
     * 获取有效通知，不存在或已删除时抛出统一业务异常。
     */
    private SysNotice getAvailableNotice(Long id) {
        SysNotice notice = sysNoticeService.getById(id);
        if (notice == null || Integer.valueOf(1).equals(notice.getIsDeleted())) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "通知不存在");
        }
        return notice;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }
}
