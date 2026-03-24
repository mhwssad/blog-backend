package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysUserFootprint;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.mapper.SysUserFootprintMapper;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.user.UserFootprintPageQuery;
import com.cybzacg.blogbackend.module.content.model.user.UserFootprintVO;
import com.cybzacg.blogbackend.module.content.service.UserFootprintService;
import com.cybzacg.blogbackend.utils.IPUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFootprintServiceImpl implements UserFootprintService {
    private final SysUserFootprintMapper sysUserFootprintMapper;
    private final com.cybzacg.blogbackend.module.content.service.SysUserFootprintService sysUserFootprintService;
    private final BlogArticleService blogArticleService;
    private final ArticleAccessControlService articleAccessControlService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public PageResult<UserFootprintVO> pageFootprints(UserFootprintPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        Page<SysUserFootprint> page = sysUserFootprintService.page(new Page<>(query.getCurrent(), query.getSize()),
                new LambdaQueryWrapper<SysUserFootprint>()
                        .eq(SysUserFootprint::getUserId, userId)
                        .eq(query.getTargetType() != null, SysUserFootprint::getTargetType, query.getTargetType())
                        .orderByDesc(SysUserFootprint::getVisitedAt)
                        .orderByDesc(SysUserFootprint::getId));
        List<UserFootprintVO> records = page.getRecords().stream().map(contentModelMapper::toUserFootprintVO).toList();
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFootprint(Long id) {
        Long userId = SecurityUtils.requireUserId();
        SysUserFootprint footprint = sysUserFootprintService.getById(id);
        ExceptionThrowerCore.throwBusinessIf(footprint == null || !userId.equals(footprint.getUserId()), ResultErrorCode.ILLEGAL_ARGUMENT, "足迹不存在");
        sysUserFootprintService.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearFootprints() {
        Long userId = SecurityUtils.requireUserId();
        sysUserFootprintService.remove(new LambdaQueryWrapper<SysUserFootprint>().eq(SysUserFootprint::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordArticleFootprint(Long articleId, HttpServletRequest request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return;
        }
        BlogArticle article = blogArticleService.getById(articleId);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            return;
        }
        articleAccessControlService.validateArticleAccess(article, userId);

        SysUserFootprint footprint = contentModelMapper.toArticleFootprint(
                userId,
                article,
                IPUtils.getIpAddr(request),
                request != null ? request.getHeader("User-Agent") : null,
                new java.util.Date());
        sysUserFootprintMapper.upsertFootprint(footprint);
    }
}



