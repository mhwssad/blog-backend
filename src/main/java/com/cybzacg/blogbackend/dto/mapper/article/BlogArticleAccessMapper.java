package com.cybzacg.blogbackend.dto.mapper.article;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleAccess;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liujian
 * @description 针对表【blog_article_access(文章访问权限表（仅access_level=4时使用）)】的数据库操作Mapper
 * @createDate 2026-03-18 20:46:54
 * @Entity com.cybzacg.blogbackend.domain.BlogArticleAccess
 */
@Mapper
public interface BlogArticleAccessMapper
    extends BaseMapper<BlogArticleAccess> {}
