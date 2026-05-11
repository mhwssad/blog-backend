package com.cybzacg.blogbackend.dto.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AiChatMessage Mapper。
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {}
