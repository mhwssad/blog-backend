package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value = "sys_collection_folder")
@Data
public class SysCollectionFolder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String folderName;

    private String folderType;

    private String description;

    private Integer isPublic;

    private Integer isDefault;

    private Integer sortOrder;

    private Integer collectionCount;

    private Date createdAt;

    private Date updatedAt;
}
