package com.cybzacg.blogbackend.module.article.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "文章访问授权项")
public class ArticleAccessItem {
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "授权类型")
    private Integer accessType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "授权原因")
    private String grantReason;
}
