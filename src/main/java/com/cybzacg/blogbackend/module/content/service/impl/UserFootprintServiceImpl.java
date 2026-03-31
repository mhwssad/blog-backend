package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysUserFootprint;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.user.UserFootprintPageQuery;
import com.cybzacg.blogbackend.module.content.model.user.UserFootprintVO;
import com.cybzacg.blogbackend.module.content.repository.SysUserFootprintRepository;
import com.cybzacg.blogbackend.module.content.service.UserFootprintService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.RequestContextUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFootprintServiceImpl implements UserFootprintService {
    private final SysUserFootprintRepository sysUserFootprintRepository;
    private final BlogArticleService blogArticleService;
    private final ArticleAccessControlService articleAccessControlService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public PageResult<UserFootprintVO> pageFootprints(UserFootprintPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        Page<SysUserFootprint> page = sysUserFootprintRepository.pageByUserIdAndTargetType(
                userId,
                query.getTargetType(),
                query.getCurrent(),
                query.getSize());
        List<UserFootprintVO> records = page.getRecords().stream().map(contentModelMapper::toUserFootprintVO).toList();
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFootprint(Long id) {
        Long userId = SecurityUtils.requireUserId();
        SysUserFootprint footprint = sysUserFootprintRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(footprint == null || !userId.equals(footprint.getUserId()), ResultErrorCode.ILLEGAL_ARGUMENT, "足迹不存在");
        sysUserFootprintRepository.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearFootprints() {
        Long userId = SecurityUtils.requireUserId();
        sysUserFootprintRepository.removeByUserId(userId);
    }

    /**
     * 记录文章浏览足迹，直接依赖数据库唯一键和 UPSERT 语义收口并发访问，避免同一用户同一文章产生重复记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordArticleFootprint(Long articleId) {
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
                RequestContextUtils.getClientIp(),
                RequestContextUtils.getUserAgent(),
                new java.util.Date());
        sysUserFootprintRepository.upsertFootprint(footprint);
    }
}
