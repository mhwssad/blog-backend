package com.cybzacg.blogbackend.module.auth.notice.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.notice.SysUserNotice;
import com.cybzacg.blogbackend.dto.mapper.notice.SysUserNoticeMapper;
import com.cybzacg.blogbackend.module.auth.notice.repository.SysUserNoticeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户通知关系 Repository 实现，基于 MyBatis-Plus。
 */
@Repository
public class SysUserNoticeRepositoryImpl extends ServiceImpl<SysUserNoticeMapper, SysUserNotice>
        implements SysUserNoticeRepository {

    /**
     * 根据通知 ID 删除所有关联的用户通知记录。
     */
    @Override
    public void deleteByNoticeId(Long noticeId) {
        remove(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getNoticeId, noticeId));
    }

    /**
     * 根据用户 ID 查询其未删除的通知关联列表。
     */
    @Override
    public List<SysUserNotice> findByUserId(Long userId) {
        return list(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsDeleted, 0));
    }

    /**
     * 查询指定用户与通知的最新一条关联记录，按 ID 降序取第一条。
     */
    @Override
    public Optional<SysUserNotice> findLatestByNoticeIdAndUserId(Long noticeId, Long userId) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getNoticeId, noticeId)
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsDeleted, 0)
                .orderByDesc(SysUserNotice::getId)
                .last("limit 1"), false));
    }

    /**
     * 判断指定用户与通知之间是否已存在未删除的关联记录。
     */
    @Override
    public boolean existsByNoticeIdAndUserId(Long noticeId, Long userId) {
        return exists(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getNoticeId, noticeId)
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsDeleted, 0));
    }
}
