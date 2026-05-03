package com.cybzacg.blogbackend.module.file.model.admin;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.core.web.PageQuery;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台上传任务分页查询条件")
public class FileTaskPageQuery extends PageQuery {
    @Schema(description = "上传用户ID")
    private Long uploadUserId;
    @EnumValue(enumClass = TaskStatusEnum.class, message = "任务状态值无效")
    @Schema(description = "任务状态")
    private Integer taskStatus;
    @Schema(description = "是否秒传")
    private Integer isQuickUpload;
    @Schema(description = "是否分片")
    private Integer isChunked;
}
