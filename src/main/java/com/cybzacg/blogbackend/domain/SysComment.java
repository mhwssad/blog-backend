package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value = "sys_comment")
@Data
public class SysComment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long targetId;

    private String targetType;

    private String content;

    private String images;

    private Long userId;

    private Long rootId;

    private Long parentId;

    private Integer likeCount;

    private Integer replyCount;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
