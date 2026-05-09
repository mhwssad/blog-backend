package com.cybzacg.blogbackend.module.article.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "文章访问授权项")
public class ArticleAccessItem {
    @NotNull(message = "授权用户不能为空")
    @Schema(description = "用户ID")
    private Long userId;
    @Min(value = 1, message = "访问类型必须为 1 或 2")
    @Max(value = 2, message = "访问类型必须为 1 或 2")
    @Schema(description = "授权类型")
    private Integer accessType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Size(max = 256, message = "授权原因最多256个字符")
    @Schema(description = "授权原因")
    private String grantReason;
}
