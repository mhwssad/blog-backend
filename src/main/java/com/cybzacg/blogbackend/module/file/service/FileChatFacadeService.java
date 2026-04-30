package com.cybzacg.blogbackend.module.file.service;

import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileInfo;

/**
 * 文件域对聊天模块暴露的稳定 facade。
 */
public interface FileChatFacadeService {

    /**
     * 校验文件业务引用可被聊天消息消费，并返回真实文件信息。
     */
    FileInfo requireSendableChatFile(Long userId, Long businessId, String chatReferenceType);

    /**
     * 将临时文件引用或未绑定的聊天引用收口为指定聊天消息引用。
     */
    FileBusinessInfo bindChatMessageReference(Long userId,
                                              Long businessId,
                                              Long messageId,
                                              String chatReferenceType,
                                              String chatCategory);

    /**
     * 释放指定业务引用下的全部文件绑定，并同步回刷文件生命周期。
     */
    void releaseReferences(String referenceType, Long referenceId);

    /**
     * 按 ID 查询文件信息；文件不存在时返回 null。
     */
    FileInfo getFileInfo(Long fileId);
}
