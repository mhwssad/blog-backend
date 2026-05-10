package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 消息附件关联表。
 */
@Data
@TableName("ai_message_attachment")
public class AiMessageAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联 ai_chat_message.id */
    private Long messageId;
    /** 关联 file_info.id */
    private Long fileId;
    /** 文件类型：image/document/audio/video/other */
    private String fileType;
    /** 文件 MIME 类型 */
    private String mimeType;
    private LocalDateTime createdAt;
}
