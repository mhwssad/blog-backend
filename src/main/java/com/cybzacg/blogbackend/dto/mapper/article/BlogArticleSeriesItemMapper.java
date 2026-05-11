package com.cybzacg.blogbackend.dto.mapper.article;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleSeriesItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章系列关联 Mapper。
 */
@Mapper
public interface BlogArticleSeriesItemMapper
    extends BaseMapper<BlogArticleSeriesItem> {}
