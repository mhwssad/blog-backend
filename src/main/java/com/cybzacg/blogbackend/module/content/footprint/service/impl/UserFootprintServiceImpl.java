package com.cybzacg.blogbackend.module.content.footprint.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.content.SysUserFootprint;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.content.footprint.model.user.UserFootprintPageQuery;
import com.cybzacg.blogbackend.module.content.footprint.model.user.UserFootprintVO;
import com.cybzacg.blogbackend.module.content.footprint.repository.SysUserFootprintRepository;
import com.cybzacg.blogbackend.module.content.footprint.service.UserFootprintService;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelConvert;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.RequestContextUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户足迹服务实现。
 *
 * <p>负责用户浏览足迹的分页查询、单条删除、全部清除以及自动记录文章浏览行为。
 */
@Service
@RequiredArgsConstructor
public class UserFootprintServiceImpl implements UserFootprintService {
    private final SysUserFootprintRepository sysUserFootprintRepository;
    private final ArticleContentFacadeService articleContentFacadeService;
    private final ContentModelConvert contentModelConvert;

    /**
     * 分页查询当前用户的浏览足迹列表。
     */
    @Override
    public PageResult<UserFootprintVO> pageFootprints(UserFootprintPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        Page<SysUserFootprint> page = sysUserFootprintRepository.pageByUserIdAndTargetType(
                userId,
                query.getTargetType(),
                query.getCurrent(),
                query.getSize());
        List<UserFootprintVO> records = page.getRecords().stream().map(contentModelConvert::toUserFootprintVO).toList();
        return PageResult.of(page, records);
    }

    /**
     * 删除当前用户的单条足迹记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFootprint(Long id) {
        Long userId = SecurityUtils.requireUserId();
        SysUserFootprint footprint = sysUserFootprintRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(footprint == null || !userId.equals(footprint.getUserId()), ResultErrorCode.ILLEGAL_ARGUMENT, "足迹不存在");
        sysUserFootprintRepository.removeById(id);
    }

    /**
     * 清除当前用户的所有浏览足迹。
     */
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
        BlogArticle article = articleContentFacadeService.findAccessiblePublishedArticle(articleId, userId);
        if (article == null) {
            return;
        }

        SysUserFootprint footprint = contentModelConvert.toArticleFootprint(
                userId,
                article,
                RequestContextUtils.getClientIp(),
                RequestContextUtils.getUserAgent(),
                LocalDateTime.now());
        sysUserFootprintRepository.upsertFootprint(footprint);
    }
}
