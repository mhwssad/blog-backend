package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.module.auth.model.admin.SysNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.UserNoticePageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 系统通知 Repository。
 */
public interface SysNoticeRepository extends IService<SysNotice> {
    Page<SysNotice> pageByAdminConditions(SysNoticePageQuery query);

    Page<SysNotice> pageInboxNotices(UserNoticePageQuery query,
                                     Collection<Long> targetNoticeIds,
                                     Collection<Long> readNoticeIds,
                                     Collection<Long> unreadTargetNoticeIds);

    long countGlobalUnread(Collection<Long> readNoticeIds);

    long countTargetedUnread(Collection<Long> unreadTargetNoticeIds);

    List<SysNotice> findGlobalUnread(Collection<Long> existingNoticeIds);
}
