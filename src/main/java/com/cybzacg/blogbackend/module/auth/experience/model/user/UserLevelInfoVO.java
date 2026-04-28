package com.cybzacg.blogbackend.module.auth.experience.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 用户等级信息视图。
 */
@Data
@Builder
@Schema(description = "用户等级信息")
public class UserLevelInfoVO {

    @Schema(description = "当前等级")
    private Integer level;

    @Schema(description = "等级称号")
    private String title;

    @Schema(description = "当前经验值")
    private Integer experiencePoints;

    @Schema(description = "下一等级所需经验")
    private Integer nextLevelThreshold;

    @Schema(description = "当前等级阈值")
    private Integer currentLevelThreshold;
}
