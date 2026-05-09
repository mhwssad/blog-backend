
package com.cybzacg.blogbackend.module.article.model.admin;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "文章新增/修改请求")
public class ArticleSaveRequest {
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 128, message = "文章标题长度不能超过128")
    @Schema(description = "文章标题")
    private String title;

    @Size(max = 2000, message = "文章摘要长度不能超过2000")
    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章内容")
    private String content;

    @Size(max = 512, message = "封面地址长度不能超过512")
    @Schema(description = "封面图地址")
    private String coverImage;

    @NotNull(message = "作者不能为空")
    @Schema(description = "作者ID")
    private Long authorId;

    @Min(value = 0, message = "置顶标识必须为 0 或 1")
    @Max(value = 1, message = "置顶标识必须为 0 或 1")
    @Schema(description = "是否置顶")
    private Integer isTop;
    @Min(value = 0, message = "推荐标识必须为 0 或 1")
    @Max(value = 1, message = "推荐标识必须为 0 或 1")
    @Schema(description = "是否推荐")
    private Integer isRecommend;
    @Min(value = 0, message = "原创标识必须为 0 或 1")
    @Max(value = 1, message = "原创标识必须为 0 或 1")
    @Schema(description = "是否原创")
    private Integer isOriginal;

    @Size(max = 512, message = "来源地址长度不能超过512")
    @Schema(description = "来源地址")
    private String sourceUrl;

    @Min(value = 0, message = "文章状态必须在 0-2 之间")
    @Max(value = 2, message = "文章状态必须在 0-2 之间")
    @Schema(description = "文章状态")
    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "定时发布时间")
    private LocalDateTime scheduledPublishTime;

    @Min(value = 0, message = "访问级别必须在 0-4 之间")
    @Max(value = 4, message = "访问级别必须在 0-4 之间")
    @Schema(description = "访问级别")
    private Integer accessLevel;

    @EnumValue(enumClass = ArticleVisibilityScopeEnum.class, message = "可见范围值无效")
    @Schema(description = "可见范围：0-公开，1-仅自己可见，2-白名单可见，3-登录可见")
    private Integer visibilityScope;

    @Size(max = 256, message = "备注长度不能超过256")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "分类ID列表")
    private List<Long> categoryIds;
    @Schema(description = "标签ID列表")
    private List<Long> tagIds;
    @Valid
    @Schema(description = "访问授权列表")
    private List<ArticleAccessItem> accessList;
}
