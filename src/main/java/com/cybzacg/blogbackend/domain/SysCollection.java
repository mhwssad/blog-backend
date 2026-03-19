package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value = "sys_collection")
@Data
public class SysCollection {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long folderId;

    private Long targetId;

    private String targetType;

    private String remark;

    private String targetTitle;

    private String targetUrl;

    private Date createdAt;
}
