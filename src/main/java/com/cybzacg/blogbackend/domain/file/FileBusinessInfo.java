package com.cybzacg.blogbackend.domain.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件业务引用关系表。
 */
@Data
@TableName("file_business_info")
public class FileBusinessInfo {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    private Long fileId;
    /**
     * 使用者ID
     */
    private Long userId;
    /**
     * 业务引用类型（avatar-头像，chat_message-聊天消息文件，article_attachment-文章附件，temp-临时文件）
     */
    private String referenceType;
    /**
     * 业务引用ID
     */
    private Long referenceId;
    /**
     * 来源IP地址
     */
    private String sourceIp;
    /**
     * 是否公开：0-私密，1-公开
     */
    private Integer isPublic;
    /**
     * 文件分类
     */
    private String category;
    /**
     * 备注
     */
    private String remark;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
