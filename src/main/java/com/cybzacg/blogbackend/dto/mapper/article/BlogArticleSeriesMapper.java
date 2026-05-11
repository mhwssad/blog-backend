package com.cybzacg.blogbackend.dto.mapper.article;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleSeries;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章系列 Mapper。
 */
@Mapper
public interface BlogArticleSeriesMapper
    extends BaseMapper<BlogArticleSeries> {}
