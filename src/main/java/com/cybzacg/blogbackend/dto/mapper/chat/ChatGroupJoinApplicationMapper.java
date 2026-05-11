package com.cybzacg.blogbackend.dto.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.chat.ChatGroupJoinApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群聊入群申请 Mapper。
 */
@Mapper
public interface ChatGroupJoinApplicationMapper
    extends BaseMapper<ChatGroupJoinApplication> {}
