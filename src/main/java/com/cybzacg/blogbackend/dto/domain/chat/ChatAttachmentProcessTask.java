package com.cybzacg.blogbackend.dto.domain.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天附件异步处理任务表。
 */
@Data
@TableName("chat_attachment_process_task")
public class ChatAttachmentProcessTask {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 关联消息ID
     */
    private Long messageId;
    /**
     * 消息类型（image-图片，file-文件）
     */
    private String messageType;
    /**
     * 任务状态：0-待处理，1-处理中，2-已完成，3-失败
     */
    private Integer taskStatus;
    /**
     * 已重试次数
     */
    private Integer retryCount;
    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;
    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryAt;
    /**
     * 处理租约过期时间
     */
    private LocalDateTime leaseExpireAt;
    /**
     * 开始处理时间
     */
    private LocalDateTime startedAt;
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
    /**
     * 最近一次错误信息
     */
    private String lastError;
    /**
     * 消息快照（JSON，用于重试）
     */
    private String messageSnapshotJson;
    /**
     * 待推送用户ID列表（JSON，用于重试）
     */
    private String pushUserIdsJson;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
