package com.cybzacg.blogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminMessageItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatMessageHistoryItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天消息 Mapper。
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    Long countMessagePage(@Param("conversationId") Long conversationId,
                          @Param("userId") Long userId,
                          @Param("beforeMessageId") Long beforeMessageId);

    List<ChatMessageHistoryItem> selectMessagePage(@Param("conversationId") Long conversationId,
                                                   @Param("userId") Long userId,
                                                   @Param("beforeMessageId") Long beforeMessageId,
                                                   @Param("offset") Long offset,
                                                   @Param("size") Long size);

    ChatMessageHistoryItem selectVisibleMessageById(@Param("conversationId") Long conversationId,
                                                    @Param("userId") Long userId,
                                                    @Param("messageId") Long messageId);

    List<ChatMessageHistoryItem> selectVisibleMessagesByIds(@Param("conversationId") Long conversationId,
                                                            @Param("userId") Long userId,
                                                            @Param("messageIds") List<Long> messageIds);

    Long countAdminMessagePage(@Param("conversationId") Long conversationId,
                               @Param("query") ChatAdminMessagePageQuery query);

    List<ChatAdminMessageItem> selectAdminMessagePage(@Param("conversationId") Long conversationId,
                                                      @Param("query") ChatAdminMessagePageQuery query,
                                                      @Param("offset") Long offset,
                                                      @Param("size") Long size);

    List<ChatAdminMessageItem> selectAdminMessagesByIds(@Param("conversationId") Long conversationId,
                                                        @Param("messageIds") List<Long> messageIds);
}
