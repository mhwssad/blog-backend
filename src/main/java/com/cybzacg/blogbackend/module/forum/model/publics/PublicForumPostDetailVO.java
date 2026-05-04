package com.cybzacg.blogbackend.module.forum.model.publics;

import com.cybzacg.blogbackend.module.chat.conversation.model.user.ForumPostChannelLinkVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "公开论坛帖子详情")
public class PublicForumPostDetailVO extends PublicForumPostVO {
    @Schema(description = "帖子内容")
    private String content;
    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;
    @Schema(description = "当前用户是否已收藏")
    private Boolean collected;
    @Schema(description = "是否允许当前用户回复")
    private Boolean canReply;
    @Schema(description = "关联频道")
    private ForumPostChannelLinkVO linkedChannel;
}
