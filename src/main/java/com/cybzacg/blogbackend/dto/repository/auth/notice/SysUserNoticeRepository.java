package com.cybzacg.blogbackend.module.auth.notice.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.notice.SysUserNotice;

import java.util.List;
import java.util.Optional;

/**
 * 用户通知关系 Repository。
 * <p>封装用户与通知之间关联关系的持久化操作，提供按用户和通知查询、删除等能力。
 */
public interface SysUserNoticeRepository extends IService<SysUserNotice> {
    /**
     * 根据通知 ID 删除所有关联的用户通知记录。
     */
    void deleteByNoticeId(Long noticeId);

    /**
     * 根据用户 ID 查询其未删除的通知关联列表。
     */
    List<SysUserNotice> findByUserId(Long userId);

    /**
     * 查询指定用户与通知的最新一条关联记录。
     */
    Optional<SysUserNotice> findLatestByNoticeIdAndUserId(Long noticeId, Long userId);

    /**
     * 判断指定用户与通知之间是否已存在未删除的关联记录。
     */
    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);
}
