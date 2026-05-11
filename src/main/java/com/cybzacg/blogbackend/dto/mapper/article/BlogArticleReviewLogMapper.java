package com.cybzacg.blogbackend.dto.mapper.article;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleReviewLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章审核记录 Mapper。
 */
@Mapper
public interface BlogArticleReviewLogMapper
    extends BaseMapper<BlogArticleReviewLog> {}
