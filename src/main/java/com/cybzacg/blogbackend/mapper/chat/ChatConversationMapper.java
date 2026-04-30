package com.cybzacg.blogbackend.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatConversationListItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天会话 Mapper。
 */
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {
    Long countConversationPage(@Param("userId") Long userId, @Param("keyword") String keyword);

    List<ChatConversationListItem> selectConversationPage(@Param("userId") Long userId,
                                                          @Param("keyword") String keyword,
                                                          @Param("offset") Long offset,
                                                          @Param("size") Long size);

    ChatConversationListItem selectConversationDetail(@Param("conversationId") Long conversationId,
                                                      @Param("userId") Long userId);

    Long countSearchableGroupPage(@Param("userId") Long userId,
                                  @Param("keyword") String keyword,
                                  @Param("categoryCode") String categoryCode);

    List<ChatConversationListItem> selectSearchableGroupPage(@Param("userId") Long userId,
                                                             @Param("keyword") String keyword,
                                                             @Param("categoryCode") String categoryCode,
                                                             @Param("offset") Long offset,
                                                             @Param("size") Long size);

    Long countAdminConversationPage(@Param("query") ChatAdminConversationPageQuery query);

    List<ChatAdminConversationListItem> selectAdminConversationPage(@Param("query") ChatAdminConversationPageQuery query,
                                                                    @Param("offset") Long offset,
                                                                    @Param("size") Long size);

    ChatAdminConversationListItem selectAdminConversationDetail(@Param("conversationId") Long conversationId);
}
