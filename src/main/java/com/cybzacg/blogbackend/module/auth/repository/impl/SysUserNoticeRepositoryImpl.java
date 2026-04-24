package com.cybzacg.blogbackend.module.auth.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.mapper.SysUserNoticeMapper;
import com.cybzacg.blogbackend.module.auth.repository.SysUserNoticeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户通知关系 Repository 实现。
 */
@Repository
public class SysUserNoticeRepositoryImpl extends ServiceImpl<SysUserNoticeMapper, SysUserNotice>
        implements SysUserNoticeRepository {

    @Override
    public void deleteByNoticeId(Long noticeId) {
        remove(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getNoticeId, noticeId));
    }

    @Override
    public List<SysUserNotice> findByUserId(Long userId) {
        return list(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsDeleted, 0));
    }

    @Override
    public Optional<SysUserNotice> findLatestByNoticeIdAndUserId(Long noticeId, Long userId) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getNoticeId, noticeId)
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsDeleted, 0)
                .orderByDesc(SysUserNotice::getId)
                .last("limit 1"), false));
    }

    @Override
    public boolean existsByNoticeIdAndUserId(Long noticeId, Long userId) {
        return exists(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getNoticeId, noticeId)
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsDeleted, 0));
    }
}
