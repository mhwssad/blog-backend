package com.cybzacg.blogbackend.module.file.model.user;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.core.web.PageQuery;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户上传任务分页查询条件")
public class UserFileTaskPageQuery extends PageQuery {
    @EnumValue(enumClass = TaskStatusEnum.class, message = "任务状态值不合法")
    @Schema(description = "任务状态")
    private Integer taskStatus;
    @Schema(description = "是否秒传 0-否，1-是")
    private Integer isQuickUpload;
    @Schema(description = "是否分片 0-否，1-是")
    private Integer isChunked;
}
