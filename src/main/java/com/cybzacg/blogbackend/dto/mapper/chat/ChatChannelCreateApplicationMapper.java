package com.cybzacg.blogbackend.dto.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.chat.ChatChannelCreateApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 频道创建申请 Mapper。
 */
@Mapper
public interface ChatChannelCreateApplicationMapper
    extends BaseMapper<ChatChannelCreateApplication> {}
