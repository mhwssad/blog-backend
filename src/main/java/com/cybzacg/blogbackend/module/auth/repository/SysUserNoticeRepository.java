package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysUserNotice;

import java.util.List;
import java.util.Optional;

/**
 * 用户通知关系 Repository。
 */
public interface SysUserNoticeRepository extends IService<SysUserNotice> {
    void deleteByNoticeId(Long noticeId);

    List<SysUserNotice> findByUserId(Long userId);

    Optional<SysUserNotice> findLatestByNoticeIdAndUserId(Long noticeId, Long userId);

    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);
}
