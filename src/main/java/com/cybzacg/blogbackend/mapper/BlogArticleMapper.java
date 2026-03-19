package com.cybzacg.blogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.BlogArticle;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author liujian
* @description 针对表【blog_article(文章表)】的数据库操作Mapper
* @createDate 2026-03-18 20:46:54
* @Entity com.cybzacg.blogbackend.domain.BlogArticle
*/
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {
    List<BlogArticle> selectAdminPage();

    List<BlogArticle> selectPublishedPage();

    BlogArticle selectArticleDetailById(@Param("id") Long id);
}
