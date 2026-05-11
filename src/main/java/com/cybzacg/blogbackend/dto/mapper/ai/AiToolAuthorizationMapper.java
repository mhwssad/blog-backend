package com.cybzacg.blogbackend.dto.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.ai.AiToolAuthorization;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 工具授权 Mapper。
 */
@Mapper
public interface AiToolAuthorizationMapper
    extends BaseMapper<AiToolAuthorization> {}
