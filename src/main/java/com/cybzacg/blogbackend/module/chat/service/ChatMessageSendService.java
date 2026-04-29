package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatSendFileRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatSendTextRequest;

/**
 * 消息发送子服务：文本消息与文件消息的发送逻辑。
 */
public interface ChatMessageSendService {

    ChatMessageVO sendTextMessage(Long userId, ChatSendTextRequest request);

    ChatMessageVO sendFileMessage(Long userId, ChatSendFileRequest request);
}
