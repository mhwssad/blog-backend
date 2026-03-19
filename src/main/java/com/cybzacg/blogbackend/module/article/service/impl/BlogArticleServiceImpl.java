package com.cybzacg.blogbackend.module.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.mapper.BlogArticleMapper;
import org.springframework.stereotype.Service;

/**
* @author liujian
* @description 针对表【blog_article(文章表)】的数据库操作Service实现
* @createDate 2026-03-18 20:46:54
*/
@Service
public class BlogArticleServiceImpl extends ServiceImpl<BlogArticleMapper, BlogArticle>
    implements BlogArticleService{

}




