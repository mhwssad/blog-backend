package com.cybzacg.blogbackend.dto.domain.content;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏夹。
 */
@TableName(value = "sys_collection_folder")
@Data
public class SysCollectionFolder {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 收藏夹名称
     */
    private String folderName;

    /**
     * 收藏夹类型（article-文章）
     */
    private String folderType;

    /**
     * 收藏夹描述
     */
    private String description;

    /**
     * 是否公开：0-私密，1-公开
     */
    private Integer isPublic;

    /**
     * 是否默认收藏夹：0-否，1-是
     */
    private Integer isDefault;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 收藏数量（冗余）
     */
    private Integer collectionCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
