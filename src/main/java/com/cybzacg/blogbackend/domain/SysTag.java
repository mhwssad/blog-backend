package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value = "sys_tag")
@Data
public class SysTag {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String color;

    private Date createdAt;
}
