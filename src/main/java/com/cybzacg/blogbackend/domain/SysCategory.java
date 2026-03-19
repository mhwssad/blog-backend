package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value = "sys_category")
@Data
public class SysCategory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    private String code;

    private String type;

    private String ancestors;

    private Integer level;

    private Integer sortOrder;

    private String icon;

    private String description;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
