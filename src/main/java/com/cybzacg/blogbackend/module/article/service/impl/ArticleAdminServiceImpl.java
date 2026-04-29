package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminCrudService;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminModerationService;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文章后台管理服务门面。
 *
 * <p>委托所有文章后台操作到对应的子服务，保持事务边界和方法签名不变。
 */
@Service
@RequiredArgsConstructor
public class ArticleAdminServiceImpl implements ArticleAdminService {

    private final ArticleAdminCrudService articleAdminCrudService;
    private final ArticleAdminModerationService articleAdminModerationService;

    @Override
    public PageResult<ArticleAdminVO> pageArticles(ArticleAdminPageQuery query) {
        return articleAdminCrudService.pageArticles(query);
    }

    @Override
    public ArticleDetailVO getArticle(Long id) {
        return articleAdminCrudService.getArticle(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleDetailVO createArticle(ArticleSaveRequest request) {
        return articleAdminCrudService.createArticle(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleDetailVO updateArticle(Long id, ArticleSaveRequest request) {
        return articleAdminCrudService.updateArticle(id, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        articleAdminCrudService.updateStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id) {
        articleAdminCrudService.deleteArticle(id);
    }

    @Override
    public void toggleTop(Long id, boolean enabled, Long operatorId, String ip, String ua) {
        articleAdminModerationService.toggleTop(id, enabled, operatorId, ip, ua);
    }

    @Override
    public void toggleRecommend(Long id, boolean enabled, Long operatorId, String ip, String ua) {
        articleAdminModerationService.toggleRecommend(id, enabled, operatorId, ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignAccess(Long id, List<ArticleAccessItem> accessList) {
        articleAdminModerationService.assignAccess(id, accessList);
    }
}
