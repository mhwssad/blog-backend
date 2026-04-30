package com.cybzacg.blogbackend.domain.content;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签。
 */
@TableName(value = "sys_tag")
@Data
public class SysTag {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签颜色（十六进制色值）
     */
    private String color;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
