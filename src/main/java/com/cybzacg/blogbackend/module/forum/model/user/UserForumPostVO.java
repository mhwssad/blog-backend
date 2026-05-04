package com.cybzacg.blogbackend.module.forum.model.user;

import com.cybzacg.blogbackend.module.forum.model.publics.PublicForumPostVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户侧论坛帖子列表项")
public class UserForumPostVO extends PublicForumPostVO {
}
