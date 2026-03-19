package com.cybzacg.blogbackend.module.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.BlogArticleCategory;
import com.cybzacg.blogbackend.module.article.service.BlogArticleCategoryService;
import com.cybzacg.blogbackend.mapper.BlogArticleCategoryMapper;
import org.springframework.stereotype.Service;

/**
* @author liujian
* @description 针对表【blog_article_category(文章-分类关联表（多对多）)】的数据库操作Service实现
* @createDate 2026-03-18 20:46:54
*/
@Service
public class BlogArticleCategoryServiceImpl extends ServiceImpl<BlogArticleCategoryMapper, BlogArticleCategory>
    implements BlogArticleCategoryService{

}




