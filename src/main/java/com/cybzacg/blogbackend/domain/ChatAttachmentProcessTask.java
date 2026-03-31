package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 聊天附件异步处理任务表。
 */
@Data
@TableName("chat_attachment_process_task")
public class ChatAttachmentProcessTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private String messageType;
    private Integer taskStatus;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Date nextRetryAt;
    private Date leaseExpireAt;
    private Date startedAt;
    private Date completedAt;
    private String lastError;
    private String messageSnapshotJson;
    private String pushUserIdsJson;
    private Date createdAt;
    private Date updatedAt;
}
