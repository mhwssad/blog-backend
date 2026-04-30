package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;

import java.util.List;

/**
 * 文章后台审核与运营服务接口。
 */
public interface ArticleAdminModerationService {

    void toggleTop(Long id, boolean enabled, Long operatorId, String ip, String ua);

    void toggleRecommend(Long id, boolean enabled, Long operatorId, String ip, String ua);

    void assignAccess(Long id, List<ArticleAccessItem> accessList);
}