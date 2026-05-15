package com.cybzacg.blogbackend.module.follow.model.admin;

import com.cybzacg.blogbackend.core.validation.AtLeastOneTrue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 异常关注关系清理请求。
 */
@Data
@AtLeastOneTrue(fields = {"cleanInactive", "cleanDeletedUsers", "cleanDisabledUsers"},
        message = "清理关注关系必须至少指定一个条件")
@Schema(description = "异常关注关系清理请求")
public class FollowRelationCleanRequest {
    @Schema(description = "是否清理已取关关系")
    private Boolean cleanInactive;

    @Schema(description = "是否清理任一端已删除的关系")
    private Boolean cleanDeletedUsers;

    @Schema(description = "是否清理任一端已禁用的关系")
    private Boolean cleanDisabledUsers;
}
