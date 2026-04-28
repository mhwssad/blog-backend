package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.article.model.admin.*;

import java.util.List;

/**
 * 文章后台管理服务接口。
 *
 * <p>定义文章后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface ArticleAdminService {
    /**
     * 按后台筛选条件分页查询文章列表。
     */
    PageResult<ArticleAdminVO> pageArticles(ArticleAdminPageQuery query);

    /**
     * 查询后台文章详情，返回分类、标签和访问授权等完整信息。
     */
    ArticleDetailVO getArticle(Long id);

    /**
     * 新建文章并同步维护分类、标签和访问授权关系。
     */
    ArticleDetailVO createArticle(ArticleSaveRequest request);

    /**
     * 更新文章主体信息，并重建分类、标签和访问授权关系。
     */
    ArticleDetailVO updateArticle(Long id, ArticleSaveRequest request);

    /**
     * 更新文章发布状态。
     */
    void updateStatus(Long id, Integer status);

    /**
     * 为指定用户可见文章单独分配访问授权。
     */
    void assignAccess(Long id, List<ArticleAccessItem> accessList);

    /**
     * 删除文章及其全部关联数据。
     */
    void deleteArticle(Long id);

    /**
     * 切换文章置顶状态。
     */
    void toggleTop(Long id, boolean enabled, Long operatorId, String ip, String ua);

    /**
     * 切换文章推荐状态。
     */
    void toggleRecommend(Long id, boolean enabled, Long operatorId, String ip, String ua);
}
