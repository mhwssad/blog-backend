package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSourceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识同步触发请求。
 */
@Data
@Schema(description = "知识同步触发请求")
public class AiKnowledgeSyncTriggerRequest {
    @NotBlank(message = "知识源类型不能为空")
    @EnumValue(enumClass = AiKnowledgeSourceTypeEnum.class, method = "getCode", message = "知识源类型无效")
    @Schema(description = "知识源类型")
    private String sourceType;

    @Pattern(regexp = "full_sync|incremental|single_entry", message = "任务类型必须是 full_sync、incremental 或 single_entry")
    @Schema(description = "任务类型，默认 full_sync")
    private String taskType = "full_sync";

    @Schema(description = "来源对象ID，single_entry 时必填")
    private Long sourceId;

    @Size(max = 512, message = "备注最多512个字符")
    @Schema(description = "备注")
    private String remark;
}
