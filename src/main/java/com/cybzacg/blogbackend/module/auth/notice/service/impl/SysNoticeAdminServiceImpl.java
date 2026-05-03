package com.cybzacg.blogbackend.module.auth.notice.service.impl;

import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.notice.SysNotice;
import com.cybzacg.blogbackend.domain.notice.SysUserNotice;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.notice.convert.SysNoticeModelConvert;
import com.cybzacg.blogbackend.module.auth.notice.model.admin.SysNoticeAdminVO;
import com.cybzacg.blogbackend.module.auth.notice.model.admin.SysNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.notice.model.admin.SysNoticeSaveRequest;
import com.cybzacg.blogbackend.module.auth.notice.repository.SysNoticeRepository;
import com.cybzacg.blogbackend.module.auth.notice.repository.SysUserNoticeRepository;
import com.cybzacg.blogbackend.module.auth.notice.service.SysNoticeAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.IdCollectionUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final SysNoticeRepository sysNoticeRepository;
    private final SysUserNoticeRepository sysUserNoticeRepository;
    private final SysUserRepository sysUserRepository;
    private final SysNoticeModelConvert sysNoticeModelConvert;
    private final SysNoticeFactory sysNoticeFactory;

    /**
     * 分页查询系统通知列表。
     */
    @Override
    public PageResult<SysNoticeAdminVO> pageNotices(SysNoticePageQuery query) {
        var page = sysNoticeRepository.pageByAdminConditions(query);
        List<SysNoticeAdminVO> records = page.getRecords().stream()
                .map(notice -> sysNoticeModelConvert.toNoticeAdminVO(notice, sysNoticeModelConvert.toIdList(notice.getTargetUserIds())))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 根据 ID 获取通知详情。
     */
    @Override
    public SysNoticeAdminVO getNotice(Long id) {
        SysNotice notice = getAvailableNotice(id);
        return sysNoticeModelConvert.toNoticeAdminVO(notice, sysNoticeModelConvert.toIdList(notice.getTargetUserIds()));
    }

    /**
     * 创建通知草稿。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysNoticeAdminVO createNotice(SysNoticeSaveRequest request) {
        List<Long> targetUserIds = validateTargetUsers(request);
        SysNotice notice = new SysNotice();
        applyFields(notice, request, targetUserIds);
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_DRAFT);
        notice.setIsDeleted(0);
        sysNoticeRepository.save(notice);
        return sysNoticeModelConvert.toNoticeAdminVO(notice, targetUserIds);
    }

    /**
     * 更新通知（仅草稿状态可编辑）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysNoticeAdminVO updateNotice(Long id, SysNoticeSaveRequest request) {
        SysNotice notice = getEditableNotice(id);
        List<Long> targetUserIds = validateTargetUsers(request);
        applyFields(notice, request, targetUserIds);
        sysNoticeRepository.updateById(notice);
        return sysNoticeModelConvert.toNoticeAdminVO(notice, targetUserIds);
    }

    /**
     * 发布通知，针对指定用户类型时自动生成用户通知投递关系。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNotice(Long id) {
        SysNotice notice = getAvailableNotice(id);
        ExceptionThrowerCore.throwBusinessIfNot(Objects.equals(NoticeConstants.PUBLISH_STATUS_DRAFT, notice.getPublishStatus()), ResultErrorCode.ILLEGAL_ARGUMENT, "仅未发布通知允许发布");

        LocalDateTime now = LocalDateTime.now();
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_PUBLISHED);
        notice.setPublishTime(now);
        notice.setRevokeTime(null);
        notice.setPublisherId(SecurityUtils.getUserId());
        sysNoticeRepository.updateById(notice);
        deliverNotice(notice, now);
    }

    /**
     * 撤回已发布的通知。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeNotice(Long id) {
        SysNotice notice = getAvailableNotice(id);
        ExceptionThrowerCore.throwBusinessIfNot(Objects.equals(NoticeConstants.PUBLISH_STATUS_PUBLISHED, notice.getPublishStatus()), ResultErrorCode.ILLEGAL_ARGUMENT, "仅已发布通知允许撤回");
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_REVOKED);
        notice.setRevokeTime(LocalDateTime.now());
        sysNoticeRepository.updateById(notice);
    }

    /**
     * 软删除通知。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        SysNotice notice = getAvailableNotice(id);
        notice.setIsDeleted(1);
        sysNoticeRepository.updateById(notice);
    }

    private void deliverNotice(SysNotice notice, LocalDateTime now) {
        sysUserNoticeRepository.deleteByNoticeId(notice.getId());
        if (!Objects.equals(NoticeConstants.TARGET_SPECIFIED, notice.getTargetType())) {
            return;
        }
        List<Long> targetUserIds = sysNoticeModelConvert.toIdList(notice.getTargetUserIds());
        ExceptionThrowerCore.throwBusinessIf(targetUserIds.isEmpty(), ResultErrorCode.ILLEGAL_ARGUMENT, "指定用户通知缺少目标用户");
        List<SysUserNotice> records = targetUserIds.stream()
                .map(userId -> sysNoticeFactory.createDeliveryRecord(notice.getId(), userId, now))
                .toList();
        sysUserNoticeRepository.saveBatch(records);
    }

    private void applyFields(SysNotice notice, SysNoticeSaveRequest request, List<Long> targetUserIds) {
        notice.setTitle(StrUtils.normalize(request.getTitle()));
        notice.setContent(request.getContent());
        notice.setType(request.getType());
        notice.setLevel(StrUtils.normalize(request.getLevel()));
        notice.setTargetType(request.getTargetType());
        notice.setTargetUserIds(IdCollectionUtils.toCommaSeparatedIds(targetUserIds));
    }

    private List<Long> validateTargetUsers(SysNoticeSaveRequest request) {
        Integer targetType = request.getTargetType();
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(NoticeConstants.TARGET_ALL, targetType) && !Objects.equals(NoticeConstants.TARGET_SPECIFIED, targetType), ResultErrorCode.ILLEGAL_ARGUMENT, "通知目标类型非法");
        if (Objects.equals(NoticeConstants.TARGET_ALL, targetType)) {
            return List.of();
        }
        ExceptionThrowerCore.throwBusinessIf(request.getTargetUserIds() == null || request.getTargetUserIds().isEmpty(), ResultErrorCode.ILLEGAL_ARGUMENT, "指定用户通知至少选择一个目标用户");
        List<Long> distinctUserIds = IdCollectionUtils.distinctNonNullIds(
                request.getTargetUserIds(),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "目标用户ID不能为空");
        long count = sysUserRepository.countActiveByIds(distinctUserIds);
        ExceptionThrowerCore.throwBusinessIf(count != distinctUserIds.size(), ResultErrorCode.ILLEGAL_ARGUMENT, "存在无效目标用户");
        return distinctUserIds;
    }

    private SysNotice getEditableNotice(Long id) {
        SysNotice notice = getAvailableNotice(id);
        ExceptionThrowerCore.throwBusinessIfNot(Objects.equals(NoticeConstants.PUBLISH_STATUS_DRAFT, notice.getPublishStatus()), ResultErrorCode.ILLEGAL_ARGUMENT, "已发布或已撤回通知不允许修改");
        return notice;
    }

    private SysNotice getAvailableNotice(Long id) {
        SysNotice notice = sysNoticeRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(notice == null || Integer.valueOf(1).equals(notice.getIsDeleted()), ResultErrorCode.ILLEGAL_ARGUMENT, "通知不存在");
        return notice;
    }
}
