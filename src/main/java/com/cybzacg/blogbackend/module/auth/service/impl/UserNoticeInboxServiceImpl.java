package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.SysNoticeModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.UserNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.UserNoticeVO;
import com.cybzacg.blogbackend.module.auth.service.SysNoticeService;
import com.cybzacg.blogbackend.module.auth.service.SysUserNoticeService;
import com.cybzacg.blogbackend.module.auth.service.UserNoticeInboxService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户通知收件箱服务实现。
 *
 * <p>负责用户收件箱分页、通知详情读取、未读统计以及已读状态维护。
 */
@Service
@RequiredArgsConstructor
public class UserNoticeInboxServiceImpl implements UserNoticeInboxService {
    private final SysNoticeService sysNoticeService;
    private final SysUserNoticeService sysUserNoticeService;
    private final SysNoticeModelMapper sysNoticeModelMapper;

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

        Page<SysNotice> emptyPage = new Page<>(query.getCurrent(), query.getSize());
        Page<SysNotice> page;
        if (Objects.equals(NoticeConstants.READ_READ, query.getIsRead()) && readNoticeIds.isEmpty()) {
            page = emptyPage;
        } else {
            var wrapper = sysNoticeService.lambdaQuery()
                    .eq(SysNotice::getIsDeleted, 0)
                    .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                    .like(StringUtils.hasText(query.getTitle()), SysNotice::getTitle, query.getTitle());

            if (query.getIsRead() == null) {
                if (targetNoticeIds.isEmpty()) {
                    wrapper.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL);
                } else {
                    wrapper.and(w -> w.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                            .or()
                            .in(SysNotice::getId, targetNoticeIds));
                }
            } else if (Objects.equals(NoticeConstants.READ_READ, query.getIsRead())) {
                wrapper.in(SysNotice::getId, readNoticeIds);
            } else {
                if (readNoticeIds.isEmpty()) {
                    if (unreadTargetNoticeIds.isEmpty()) {
                        wrapper.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL);
                    } else {
                        wrapper.and(w -> w.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                                .or()
                                .in(SysNotice::getId, unreadTargetNoticeIds));
                    }
                } else if (unreadTargetNoticeIds.isEmpty()) {
                    wrapper.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                            .notIn(SysNotice::getId, readNoticeIds);
                } else {
                    wrapper.and(w -> w.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                                    .notIn(SysNotice::getId, readNoticeIds)
                                    .or()
                                    .in(SysNotice::getId, unreadTargetNoticeIds));
                }
            }

            page = wrapper.orderByDesc(SysNotice::getPublishTime)
                    .orderByDesc(SysNotice::getId)
                    .page(new Page<>(query.getCurrent(), query.getSize()));
        }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserNoticeVO getMyNotice(Long noticeId) {
        Long userId = SecurityUtils.requireUserId();
        SysNotice notice = getAccessibleNotice(userId, noticeId);
        SysUserNotice relation = markReadInternal(userId, notice);
        return sysNoticeModelMapper.toUserNoticeVO(notice, true, relation.getReadTime());
    }

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

        long globalUnread = sysNoticeService.lambdaQuery()
                .eq(SysNotice::getIsDeleted, 0)
                .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                .eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                .notIn(!readNoticeIds.isEmpty(), SysNotice::getId, readNoticeIds)
                .count();
        if (unreadTargetNoticeIds.isEmpty()) {
            return globalUnread;
        }
        long targetedUnread = sysNoticeService.lambdaQuery()
                .eq(SysNotice::getIsDeleted, 0)
                .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                .in(SysNotice::getId, unreadTargetNoticeIds)
                .count();
        return globalUnread + targetedUnread;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long noticeId) {
        Long userId = SecurityUtils.requireUserId();
        SysNotice notice = getAccessibleNotice(userId, noticeId);
        markReadInternal(userId, notice);
    }

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
                    sysUserNoticeService.updateById(item);
                });

        List<Long> readNoticeIds = relations.stream()
                .filter(item -> Objects.equals(NoticeConstants.READ_READ, item.getIsRead()))
                .map(SysUserNotice::getNoticeId)
                .distinct()
                .toList();
        List<SysNotice> unreadGlobalNotices = sysNoticeService.lambdaQuery()
                .eq(SysNotice::getIsDeleted, 0)
                .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                .eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                .notIn(!readNoticeIds.isEmpty(), SysNotice::getId, readNoticeIds)
                .list();
        if (unreadGlobalNotices.isEmpty()) {
            return;
        }
        List<SysUserNotice> newRecords = unreadGlobalNotices.stream()
                .map(notice -> {
                    SysUserNotice userNotice = new SysUserNotice();
                    userNotice.setNoticeId(notice.getId());
                    userNotice.setUserId(userId);
                    userNotice.setIsRead(NoticeConstants.READ_READ);
                    userNotice.setReadTime(now);
                    userNotice.setCreateTime(now);
                    userNotice.setUpdateTime(now);
                    userNotice.setIsDeleted(0);
                    return userNotice;
                })
                .toList();
        sysUserNoticeService.saveBatch(newRecords);
    }

    /**
     * 读取用户与通知的关联记录，作为已读状态和指定通知可见性的基础数据。
     */
    private List<SysUserNotice> listUserNoticeRelations(Long userId) {
        return sysUserNoticeService.lambdaQuery()
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsDeleted, 0)
                .list();
    }

    /**
     * 将关联记录转换为以通知 ID 为键的映射，便于分页结果快速回填已读状态。
     */
    private Map<Long, SysUserNotice> buildRelationMap(List<SysUserNotice> relations) {
        Map<Long, SysUserNotice> relationMap = new LinkedHashMap<>();
        for (SysUserNotice relation : relations) {
            relationMap.put(relation.getNoticeId(), relation);
        }
        return relationMap;
    }

    /**
     * 校验当前用户是否有权访问通知，兼容全员通知与指定用户通知两种场景。
     */
    private SysNotice getAccessibleNotice(Long userId, Long noticeId) {
        SysNotice notice = sysNoticeService.getById(noticeId);
        if (notice == null
                || Integer.valueOf(1).equals(notice.getIsDeleted())
                || !Objects.equals(NoticeConstants.PUBLISH_STATUS_PUBLISHED, notice.getPublishStatus())) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "通知不存在");
        }
        if (Objects.equals(NoticeConstants.TARGET_ALL, notice.getTargetType())) {
            return notice;
        }
        boolean exists = sysUserNoticeService.lambdaQuery()
                .eq(SysUserNotice::getNoticeId, noticeId)
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsDeleted, 0)
                .exists();
        if (!exists) {
            throw new BusinessException(ResultErrorCode.FORBIDDEN);
        }
        return notice;
    }

    /**
     * 将通知标记为已读；若是首次读取全员通知，则自动补建用户通知关系记录。
     */
    private SysUserNotice markReadInternal(Long userId, SysNotice notice) {
        Date now = new Date();
        SysUserNotice relation = sysUserNoticeService.lambdaQuery()
                .eq(SysUserNotice::getNoticeId, notice.getId())
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsDeleted, 0)
                .one();
        if (relation == null) {
            relation = new SysUserNotice();
            relation.setNoticeId(notice.getId());
            relation.setUserId(userId);
            relation.setIsRead(NoticeConstants.READ_READ);
            relation.setReadTime(now);
            relation.setCreateTime(now);
            relation.setUpdateTime(now);
            relation.setIsDeleted(0);
            sysUserNoticeService.save(relation);
            return relation;
        }
        if (!Objects.equals(NoticeConstants.READ_READ, relation.getIsRead())) {
            relation.setIsRead(NoticeConstants.READ_READ);
            relation.setReadTime(now);
            relation.setUpdateTime(now);
            sysUserNoticeService.updateById(relation);
        }
        return relation;
    }
}
