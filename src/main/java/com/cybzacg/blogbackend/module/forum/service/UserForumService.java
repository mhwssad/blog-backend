package com.cybzacg.blogbackend.module.forum.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ForumPostChannelLinkVO;
import com.cybzacg.blogbackend.module.forum.model.user.*;

/**
 * 用户侧论坛服务。
 */
public interface UserForumService {
    PageResult<UserForumPostVO> pageMyPosts(UserForumPostPageQuery query);

    UserForumPostDetailVO getMyPost(Long id);

    UserForumPostDetailVO createPost(ForumPostSaveRequest request);

    UserForumPostDetailVO updatePost(Long id, ForumPostSaveRequest request);

    void deletePost(Long id);

    void createReply(Long postId, ForumReplySaveRequest request);

    void updateReply(Long replyId, ForumReplySaveRequest request);

    void deleteReply(Long replyId);

    void likePost(Long postId);

    void unlikePost(Long postId);

    void collectPost(Long postId, ForumPostCollectRequest request);

    void uncollectPost(Long postId);

    ForumPostChannelLinkVO sharePostToChannel(Long postId, Long conversationId);
}
