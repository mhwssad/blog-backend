package com.cybzacg.blogbackend.domain.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户经验流水实体。
 */
@Data
@TableName("user_experience_log")
public class UserExperienceLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String sourceType;

    private String sourceBizId;

    private Integer xpValue;

    private String idempotentKey;

    private LocalDate logDate;

    private LocalDateTime createdAt;
}
