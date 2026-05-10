package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI Agent 定义表。
 */
@Data
@TableName("ai_agent_definition")
public class AiAgentDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** agent 名称（唯一） */
    private String name;
    /** agent 描述 */
    private String description;
    /** 系统提示词 */
    private String systemPrompt;
    /** 关联 AI 渠道配置 ID */
    private Long channelConfigId;
    /** 数据读取范围配置 JSON（复用 AiDataScopeEnum） */
    private String dataScopeJson;
    /** 0-停用，1-启用 */
    private Integer enabled;
    /** 最大对话轮次 */
    private Integer maxTurns;
    /** 扩展配置 JSON（预留） */
    private String extraConfigJson;
    /** 创建人 */
    private Long createdBy;
    /** 更新人 */
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
