package com.cybzacg.blogbackend.module.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.BlogArticleAccess;
import com.cybzacg.blogbackend.module.article.service.BlogArticleAccessService;
import com.cybzacg.blogbackend.mapper.BlogArticleAccessMapper;
import org.springframework.stereotype.Service;

/**
* @author liujian
* @description 针对表【blog_article_access(文章访问权限表（仅access_level=4时使用）)】的数据库操作Service实现
* @createDate 2026-03-18 20:46:54
*/
@Service
public class BlogArticleAccessServiceImpl extends ServiceImpl<BlogArticleAccessMapper, BlogArticleAccess>
    implements BlogArticleAccessService{

}




