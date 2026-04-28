
package com.cybzacg.blogbackend.module.article.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "是否置顶")
    private Integer isTop;
    @Schema(description = "是否推荐")
    private Integer isRecommend;
    @Schema(description = "是否原创")
    private Integer isOriginal;

    @Size(max = 512, message = "来源地址长度不能超过512")
    @Schema(description = "来源地址")
    private String sourceUrl;

    @Schema(description = "文章状态")
    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "定时发布时间")
    private LocalDateTime scheduledPublishTime;

    @Schema(description = "访问级别")
    private Integer accessLevel;

    @Schema(description = "可见范围：0-公开，1-仅自己可见，2-白名单可见，3-登录可见")
    private Integer visibilityScope;

    @Size(max = 256, message = "备注长度不能超过256")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "分类ID列表")
    private List<Long> categoryIds;
    @Schema(description = "标签ID列表")
    private List<Long> tagIds;
    @Schema(description = "访问授权列表")
    private List<ArticleAccessItem> accessList;
}
