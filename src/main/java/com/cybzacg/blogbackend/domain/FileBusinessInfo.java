package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文件业务引用关系表。
 */
@Data
@TableName("file_business_info")
public class FileBusinessInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private Long userId;
    private String referenceType;
    private Long referenceId;
    private String sourceIp;
    private Integer isPublic;
    private String category;
    private String remark;
    private Date createdAt;
    private Date updatedAt;
}
