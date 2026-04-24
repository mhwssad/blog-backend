package com.cybzacg.blogbackend.module.auth.service.impl;

import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.convert.SysNoticeModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.UserNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.UserNoticeVO;
import com.cybzacg.blogbackend.module.auth.repository.SysNoticeRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserNoticeRepository;
import com.cybzacg.blogbackend.module.auth.service.UserNoticeInboxService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户通知收件箱服务实现。
 *
 * <p>负责用户收件箱分页、通知详情读取、未读统计以及已读状态维护。
 */
@Service
@RequiredArgsConstructor
public class UserNoticeInboxServiceImpl implements UserNoticeInboxService {
    private final SysNoticeRepository sysNoticeRepository;
    private final SysUserNoticeRepository sysUserNoticeRepository;
    private final SysNoticeModelMapper sysNoticeModelMapper;

    /** 分页查询当前用户的收件箱通知，区分已读和未读状态。 */
    @Override
    public PageResult<UserNoticeVO> pageMyNotices(UserNoticePageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        List<SysUserNotice> relations = listUserNoticeRelations(userId);
        Map<Long, SysUserNotice> relationMap = buildRelationMap(relations);
        List<Long> targetNoticeIds = relations.stream().map(SysUserNotice::getNoticeId).distinct().toList();
        List<Long> readNoticeIds = relations.stream()
                .filter(item -> Objects.equals(NoticeConstants.READ_READ, item.getIsRead()))
                .map(SysUserNotice::getNoticeId)
                .distinct()
                .toList();
        List<Long> unreadTargetNoticeIds = relations.stream()
                .filter(item -> !Objects.equals(NoticeConstants.READ_READ, item.getIsRead()))
                .map(SysUserNotice::getNoticeId)
                .distinct()
                .toList();

        var page = sysNoticeRepository.pageInboxNotices(query, targetNoticeIds, readNoticeIds, unreadTargetNoticeIds);
        List<UserNoticeVO> records = page.getRecords().stream()
                .map(notice -> {
                    SysUserNotice relation = relationMap.get(notice.getId());
                    return sysNoticeModelMapper.toUserNoticeVO(notice,
                            relation != null && Objects.equals(NoticeConstants.READ_READ, relation.getIsRead()),
                            relation != null ? relation.getReadTime() : null);
                })
                .toList();
        return PageResult.of(page, records);
    }

    /** 查看通知详情，自动标记为已读。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserNoticeVO getMyNotice(Long noticeId) {
        Long userId = SecurityUtils.requireUserId();
        SysNotice notice = getAccessibleNotice(userId, noticeId);
        SysUserNotice relation = markReadInternal(userId, notice);
        return sysNoticeModelMapper.toUserNoticeVO(notice, true, relation.getReadTime());
    }

    /** 统计当前用户的未读通知数（含全局通知和指定用户通知）。 */
    @Override
    public long countUnreadNotices() {
        Long userId = SecurityUtils.requireUserId();
        List<SysUserNotice> relations = listUserNoticeRelations(userId);
        List<Long> readNoticeIds = relations.stream()
                .filter(item -> Objects.equals(NoticeConstants.READ_READ, item.getIsRead()))
                .map(SysUserNotice::getNoticeId)
                .distinct()
                .toList();
        List<Long> unreadTargetNoticeIds = relations.stream()
                .filter(item -> !Objects.equals(NoticeConstants.READ_READ, item.getIsRead()))
                .map(SysUserNotice::getNoticeId)
                .distinct()
                .toList();

        long globalUnread = sysNoticeRepository.countGlobalUnread(readNoticeIds);
        long targetedUnread = sysNoticeRepository.countTargetedUnread(unreadTargetNoticeIds);
        return globalUnread + targetedUnread;
    }

    /** 将指定通知标记为已读。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long noticeId) {
        Long userId = SecurityUtils.requireUserId();
        SysNotice notice = getAccessibleNotice(userId, noticeId);
        markReadInternal(userId, notice);
    }

    /** 将当前用户的所有未读通知批量标记为已读（含全局通知的补录）。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        Long userId = SecurityUtils.requireUserId();
        Date now = new Date();
        List<SysUserNotice> relations = listUserNoticeRelations(userId);
        relations.stream()
                .filter(item -> !Objects.equals(NoticeConstants.READ_READ, item.getIsRead()))
                .forEach(item -> {
                    item.setIsRead(NoticeConstants.READ_READ);
                    item.setReadTime(now);
                    item.setUpdateTime(now);
                    sysUserNoticeRepository.updateById(item);
                });

        Set<Long> existingNoticeIds = relations.stream()
                .map(SysUserNotice::getNoticeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<SysNotice> unreadGlobalNotices = sysNoticeRepository.findGlobalUnread(existingNoticeIds);
        for (SysNotice notice : unreadGlobalNotices) {
            markReadInternal(userId, notice);
        }
    }

    private List<SysUserNotice> listUserNoticeRelations(Long userId) {
        return sysUserNoticeRepository.findByUserId(userId);
    }

    private Map<Long, SysUserNotice> buildRelationMap(List<SysUserNotice> relations) {
        Map<Long, SysUserNotice> relationMap = new LinkedHashMap<>();
        for (SysUserNotice relation : relations) {
            relationMap.put(relation.getNoticeId(), relation);
        }
        return relationMap;
    }

    private SysNotice getAccessibleNotice(Long userId, Long noticeId) {
        SysNotice notice = sysNoticeRepository.getById(noticeId);
        ExceptionThrowerCore.throwBusinessIf(notice == null
                        || Integer.valueOf(1).equals(notice.getIsDeleted())
                        || !Objects.equals(NoticeConstants.PUBLISH_STATUS_PUBLISHED, notice.getPublishStatus()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "通知不存在");
        if (Objects.equals(NoticeConstants.TARGET_ALL, notice.getTargetType())) {
            return notice;
        }
        boolean exists = sysUserNoticeRepository.existsByNoticeIdAndUserId(noticeId, userId);
        ExceptionThrowerCore.throwBusinessIfNot(exists, ResultErrorCode.FORBIDDEN);
        return notice;
    }

    private SysUserNotice markReadInternal(Long userId, SysNotice notice) {
        Date now = new Date();
        SysUserNotice relation = findActiveRelation(userId, notice.getId());
        if (relation == null) {
            relation = new SysUserNotice();
            relation.setNoticeId(notice.getId());
            relation.setUserId(userId);
            relation.setIsRead(NoticeConstants.READ_READ);
            relation.setReadTime(now);
            relation.setCreateTime(now);
            relation.setUpdateTime(now);
            relation.setIsDeleted(0);
            try {
                sysUserNoticeRepository.save(relation);
                return relation;
            } catch (DuplicateKeyException ex) {
                relation = findActiveRelation(userId, notice.getId());
            }
        }
        ExceptionThrowerCore.throwBusinessIfNull(relation, ResultErrorCode.ILLEGAL_ARGUMENT, "通知状态更新失败");
        if (!Objects.equals(NoticeConstants.READ_READ, relation.getIsRead())) {
            relation.setIsRead(NoticeConstants.READ_READ);
            relation.setReadTime(now);
            relation.setUpdateTime(now);
            sysUserNoticeRepository.updateById(relation);
        }
        return relation;
    }

    private SysUserNotice findActiveRelation(Long userId, Long noticeId) {
        return sysUserNoticeRepository.findLatestByNoticeIdAndUserId(noticeId, userId).orElse(null);
    }
}
