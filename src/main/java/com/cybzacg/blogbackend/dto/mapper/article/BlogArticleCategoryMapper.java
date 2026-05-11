package com.cybzacg.blogbackend.dto.mapper.article;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liujian
 * @description 针对表【blog_article_category(文章-分类关联表（多对多）)】的数据库操作Mapper
 * @createDate 2026-03-18 20:46:54
 * @Entity com.cybzacg.blogbackend.domain.BlogArticleCategory
 */
@Mapper
public interface BlogArticleCategoryMapper
    extends BaseMapper<BlogArticleCategory> {}
