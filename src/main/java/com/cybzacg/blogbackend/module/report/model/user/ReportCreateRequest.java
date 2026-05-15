package com.cybzacg.blogbackend.module.report.model.user;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.enums.report.ReportTargetTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户提交举报请求。
 */
@Data
@Schema(description = "提交举报请求")
public class ReportCreateRequest {
    @NotBlank(message = "举报对象类型不能为空")
    @EnumValue(enumClass = ReportTargetTypeEnum.class, method = "getCode", message = "举报对象类型无效")
    @Schema(description = "举报对象类型：article/comment/chat_message", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetType;

    @NotNull(message = "举报对象ID不能为空")
    @Schema(description = "举报对象ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetId;

    @NotBlank(message = "举报原因不能为空")
    @Size(max = 64, message = "原因编码不能超过64字符")
    @Schema(description = "举报原因编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reasonCode;

    @Size(max = 512, message = "补充说明不能超过512字符")
    @Schema(description = "补充说明")
    private String reasonDetail;
}
