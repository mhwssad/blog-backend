package com.cybzacg.blogbackend.dto.domain.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛回复。
 */
@Data
@TableName("forum_reply")
public class ForumReply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long parentId;
    private Long rootId;
    private Long userId;
    private String content;
    /**
     * 状态：1-正常，2-隐藏，3-删除，4-审核中。
     */
    private Integer status;
    /**
     * 楼层号，按帖子内回复创建顺序递增。
     */
    private Integer floorNo;
    private Integer likeCount;
    private Integer replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
