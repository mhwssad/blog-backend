package com.cybzacg.blogbackend.domain.content;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏记录。
 */
@TableName(value = "sys_collection")
@Data
public class SysCollection {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 收藏用户ID
     */
    private Long userId;

    /**
     * 收藏夹ID
     */
    private Long folderId;

    /**
     * 收藏目标ID
     */
    private Long targetId;

    /**
     * 收藏目标类型（article-文章）
     */
    private String targetType;

    /**
     * 收藏备注
     */
    private String remark;

    /**
     * 收藏目标标题（冗余）
     */
    private String targetTitle;

    /**
     * 收藏目标链接（冗余）
     */
    private String targetUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
