package com.cybzacg.blogbackend.module.forum.model.user;

import com.cybzacg.blogbackend.module.forum.model.publics.PublicForumPostDetailVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户侧论坛帖子详情")
public class UserForumPostDetailVO extends PublicForumPostDetailVO {
}
