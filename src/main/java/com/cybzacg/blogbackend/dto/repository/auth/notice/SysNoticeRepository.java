package com.cybzacg.blogbackend.dto.repository.auth.notice;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.notice.SysNotice;
import com.cybzacg.blogbackend.module.auth.notice.model.admin.SysNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.notice.model.admin.UserNoticePageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 系统通知 Repository。
 * <p>封装通知实体的持久化操作，提供管理端分页、收件箱查询及未读统计等能力。
 */
public interface SysNoticeRepository extends IService<SysNotice> {
    /**
     * 根据管理端查询条件对未删除通知进行分页。
     */
    Page<SysNotice> pageByAdminConditions(SysNoticePageQuery query);

    /**
     * 根据用户收件箱条件进行分页，结合已读/未读和全局/定向通知进行过滤。
     */
    Page<SysNotice> pageInboxNotices(UserNoticePageQuery query,
                                     Collection<Long> targetNoticeIds,
                                     Collection<Long> readNoticeIds,
                                     Collection<Long> unreadTargetNoticeIds,
                                     boolean includeGlobalNotices);

    /**
     * 统计全局通知中用户未读的数量。
     */
    long countGlobalUnread(Collection<Long> readNoticeIds);

    /**
     * 统计定向通知中用户未读的数量。
     */
    long countTargetedUnread(Collection<Long> unreadTargetNoticeIds);

    /**
     * 查询用户未读的全局通知列表（排除已存在的通知 ID）。
     */
    List<SysNotice> findGlobalUnread(Collection<Long> existingNoticeIds);
}
