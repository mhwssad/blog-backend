package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 标签关联关系。 */
@TableName(value = "sys_tag_relation")
@Data
public class SysTagRelation {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签ID */
    private Long tagId;

    /** 关联目标ID */
    private Long targetId;

    /** 关联目标类型（article-文章） */
    private String targetType;

    /** 创建时间 */
    private Date createdAt;
}
