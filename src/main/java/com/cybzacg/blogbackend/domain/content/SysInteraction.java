package com.cybzacg.blogbackend.domain.content;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户互动记录。
 */
@TableName(value = "sys_interaction")
@Data
public class SysInteraction {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 互动目标ID
     */
    private Long targetId;

    /**
     * 互动目标类型（article-文章，comment-评论）
     */
    private String targetType;

    /**
     * 互动类型（like-点赞）
     */
    private String actionType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
