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
 * 系统通知 Repository 实现，基于 MyBatis-Plus。
 */
@Repository
public class SysNoticeRepositoryImpl extends ServiceImpl<SysNoticeMapper, SysNotice>
        implements SysNoticeRepository {

    /**
     * 根据管理端查询条件进行分页，按创建时间降序排列。
     */
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

    /**
     * 根据用户收件箱条件进行分页。
     * <p>根据 isRead 状态和通知类型（全局/定向）动态构建查询条件：
     * <ul>
     *   <li>isRead 为 null 时：查询全部已发布通知（全局 + 定向）</li>
     *   <li>isRead 为已读时：仅查询用户已读的通知 ID</li>
     *   <li>isRead 为未读时：排除已读通知后查询全局未读 + 定向未读</li>
     * </ul>
     */
    @Override
    public Page<SysNotice> pageInboxNotices(UserNoticePageQuery query,
                                            Collection<Long> targetNoticeIds,
                                            Collection<Long> readNoticeIds,
                                            Collection<Long> unreadTargetNoticeIds,
                                            boolean includeGlobalNotices) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getIsDeleted, 0)
                .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                .like(StringUtils.hasText(query.getTitle()), SysNotice::getTitle, query.getTitle());

        if (query.getIsRead() == null) {
            // 不区分已读/未读：全局通知 + 用户定向通知
            if (targetNoticeIds == null || targetNoticeIds.isEmpty()) {
                if (!includeGlobalNotices) {
                    return new Page<>(query.getCurrent(), query.getSize());
                }
                wrapper.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL);
            } else if (!includeGlobalNotices) {
                wrapper.in(SysNotice::getId, targetNoticeIds);
            } else {
                wrapper.and(w -> w.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                        .or()
                        .in(SysNotice::getId, targetNoticeIds));
            }
        } else if (Objects.equals(NoticeConstants.READ_READ, query.getIsRead())) {
            // 已读列表为空则直接返回空页
            if (readNoticeIds == null || readNoticeIds.isEmpty()) {
                return new Page<>(query.getCurrent(), query.getSize());
            }
            wrapper.in(SysNotice::getId, readNoticeIds);
        } else {
            // 未读：排除已读后取全局未读 + 定向未读
            boolean hasRead = readNoticeIds != null && !readNoticeIds.isEmpty();
            boolean hasUnreadTarget = unreadTargetNoticeIds != null && !unreadTargetNoticeIds.isEmpty();
            if (!includeGlobalNotices) {
                if (!hasUnreadTarget) {
                    return new Page<>(query.getCurrent(), query.getSize());
                }
                wrapper.in(SysNotice::getId, unreadTargetNoticeIds);
                return page(new Page<>(query.getCurrent(), query.getSize()), wrapper
                        .orderByDesc(SysNotice::getPublishTime)
                        .orderByDesc(SysNotice::getId));
            }
            if (!hasRead) {
                if (!hasUnreadTarget) {
                    wrapper.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL);
                } else {
                    wrapper.and(w -> w.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                            .or()
                            .in(SysNotice::getId, unreadTargetNoticeIds));
                }
            } else if (!hasUnreadTarget) {
                // 有已读但无定向未读：取全局通知中排除已读的
                wrapper.eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                        .notIn(SysNotice::getId, readNoticeIds);
            } else {
                // 有已读且有定向未读：合并全局未读和定向未读
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

    /**
     * 统计全局已发布通知中排除用户已读之后的未读数量。
     */
    @Override
    public long countGlobalUnread(Collection<Long> readNoticeIds) {
        return count(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getIsDeleted, 0)
                .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                .eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                .notIn(readNoticeIds != null && !readNoticeIds.isEmpty(), SysNotice::getId, readNoticeIds));
    }

    /**
     * 统计定向通知中用户未读的数量。
     */
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

    /**
     * 查询用户未读的全局通知，排除已存在的通知 ID。
     */
    @Override
    public List<SysNotice> findGlobalUnread(Collection<Long> existingNoticeIds) {
        return list(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getIsDeleted, 0)
                .eq(SysNotice::getPublishStatus, NoticeConstants.PUBLISH_STATUS_PUBLISHED)
                .eq(SysNotice::getTargetType, NoticeConstants.TARGET_ALL)
                .notIn(existingNoticeIds != null && !existingNoticeIds.isEmpty(), SysNotice::getId, existingNoticeIds));
    }
}
