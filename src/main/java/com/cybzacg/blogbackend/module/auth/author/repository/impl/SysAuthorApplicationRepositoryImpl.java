package com.cybzacg.blogbackend.module.auth.author.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.auth.SysAuthorApplication;
import com.cybzacg.blogbackend.mapper.auth.SysAuthorApplicationMapper;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.auth.author.repository.SysAuthorApplicationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 作者申请 Repository 实现。
 */
@Repository
public class SysAuthorApplicationRepositoryImpl extends ServiceImpl<SysAuthorApplicationMapper, SysAuthorApplication>
        implements SysAuthorApplicationRepository {

    /**
     * 查询用户最近一次提交的申请，优先按提交时间倒序，随后按ID倒序收口。
     */
    @Override
    public SysAuthorApplication findLatestByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<SysAuthorApplication>()
                .eq(SysAuthorApplication::getUserId, userId)
                .orderByDesc(SysAuthorApplication::getSubmittedAt)
                .orderByDesc(SysAuthorApplication::getId)
                .last("limit 1"), false);
    }

    /**
     * 分页查询用户自己的申请记录。
     */
    @Override
    public Page<SysAuthorApplication> pageByUserId(Long userId, long current, long size) {
        return page(new Page<>(current, size), new LambdaQueryWrapper<SysAuthorApplication>()
                .eq(SysAuthorApplication::getUserId, userId)
                .orderByDesc(SysAuthorApplication::getSubmittedAt)
                .orderByDesc(SysAuthorApplication::getId));
    }

    /**
     * 按后台条件分页查询作者申请记录。
     */
    @Override
    public Page<SysAuthorApplication> pageByAdminConditions(SysAuthorApplicationAdminPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysAuthorApplication>()
                .eq(query.getUserId() != null, SysAuthorApplication::getUserId, query.getUserId())
                .eq(query.getApplyStatus() != null, SysAuthorApplication::getApplyStatus, query.getApplyStatus())
                .and(StringUtils.hasText(query.getKeyword()), wrapper -> wrapper
                        .like(SysAuthorApplication::getApplyReason, query.getKeyword())
                        .or()
                        .like(SysAuthorApplication::getContentDirection, query.getKeyword())
                        .or()
                        .like(SysAuthorApplication::getIntroduction, query.getKeyword()))
                .orderByAsc(SysAuthorApplication::getApplyStatus)
                .orderByDesc(SysAuthorApplication::getSubmittedAt)
                .orderByDesc(SysAuthorApplication::getId));
    }
}
