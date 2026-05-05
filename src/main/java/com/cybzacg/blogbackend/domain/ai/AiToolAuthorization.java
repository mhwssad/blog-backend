package com.cybzacg.blogbackend.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 工具授权关系表。
 */
@Data
@TableName("ai_tool_authorization")
public class AiToolAuthorization {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long toolId;
    private String authorizationType;
    private String authorizationKey;
    private String dataScope;
    private Integer enabled;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
