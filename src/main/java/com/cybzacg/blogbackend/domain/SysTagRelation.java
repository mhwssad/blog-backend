package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value = "sys_tag_relation")
@Data
public class SysTagRelation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tagId;

    private Long targetId;

    private String targetType;

    private Date createdAt;
}
