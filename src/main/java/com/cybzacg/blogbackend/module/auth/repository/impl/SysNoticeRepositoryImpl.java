package com.cybzacg.blogbackend.module.auth.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.mapper.SysNoticeMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.UserNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.repository.SysNoticeRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 系统通知 Repository 实现。
 */
@Repository
public class SysNoticeRepositoryImpl extends ServiceImpl<SysNoticeMapper, SysNotice>
        implements SysNoticeRepository {

    @Override
    public Page<SysNotice> pageByAdminConditions(SysNoticePageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysNotice>()
                .like(StringUtils.hasText(query.getTitle()), SysNotice::getTitle, query.getTitle())
                .eq(query.getPublishStatus() != null, SysNotice::getPublishStatus, query.getPublishStatus())
                .eq(query.getTargetType() != null, SysNotice::getTargetType, query.getTargetType())
                .eq(SysNotice::getIsDeleted, 0)
                .orderByDesc(SysNotice::getCreateTime)
                .orderByDesc(SysNotice::getId));
    }

    @Override
    public Page<SysNotice> pageInboxNotices(UserNoticePageQuery query,
                                            Collection<Long> targetNoticeIds,
                                            Collection<Long> readNoticeIds,
                                            Collection<Long> unreadTargetNoticeIds) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getIsDeleted, 0)
                .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                .like(StringUtils.hasText(query.getTitle()), SysNotice::getTitle, query.getTitle());

        if (query.getIsRead() == null) {
            if (targetNoticeIds == null || targetNoticeIds.isEmpty()) {
                wrapper.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL);
            } else {
                wrapper.and(w -> w.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                        .or()
                        .in(SysNotice::getId, targetNoticeIds));
            }
        } else if (Objects.equals(NoticeConstants.READ_READ, query.getIsRead())) {
            if (readNoticeIds == null || readNoticeIds.isEmpty()) {
                return new Page<>(query.getCurrent(), query.getSize());
            }
            wrapper.in(SysNotice::getId, readNoticeIds);
        } else {
            boolean hasRead = readNoticeIds != null && !readNoticeIds.isEmpty();
            boolean hasUnreadTarget = unreadTargetNoticeIds != null && !unreadTargetNoticeIds.isEmpty();
            if (!hasRead) {
                if (!hasUnreadTarget) {
                    wrapper.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL);
                } else {
                    wrapper.and(w -> w.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                            .or()
                            .in(SysNotice::getId, unreadTargetNoticeIds));
                }
            } else if (!hasUnreadTarget) {
                wrapper.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                        .notIn(SysNotice::getId, readNoticeIds);
            } else {
                wrapper.and(w -> w.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                                .notIn(SysNotice::getId, readNoticeIds)
                                .or()
                                .in(SysNotice::getId, unreadTargetNoticeIds));
            }
        }

        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper
                .orderByDesc(SysNotice::getPublishTime)
                .orderByDesc(SysNotice::getId));
    }

    @Override
    public long countGlobalUnread(Collection<Long> readNoticeIds) {
        return count(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getIsDeleted, 0)
                .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                .eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                .notIn(readNoticeIds != null && !readNoticeIds.isEmpty(), SysNotice::getId, readNoticeIds));
    }

    @Override
    public long countTargetedUnread(Collection<Long> unreadTargetNoticeIds) {
        if (unreadTargetNoticeIds == null || unreadTargetNoticeIds.isEmpty()) {
            return 0L;
        }
        return count(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getIsDeleted, 0)
                .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                .in(SysNotice::getId, unreadTargetNoticeIds));
    }

    @Override
    public List<SysNotice> findGlobalUnread(Collection<Long> existingNoticeIds) {
        return list(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getIsDeleted, 0)
                .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                .eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                .notIn(existingNoticeIds != null && !existingNoticeIds.isEmpty(), SysNotice::getId, existingNoticeIds));
    }
}
