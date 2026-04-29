package com.cybzacg.blogbackend.module.content.comment.service;

import com.cybzacg.blogbackend.module.content.comment.model.user.CommentSaveRequest;

/**
 * 用户评论服务接口。
 *
 * <p>定义用户评论相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface UserCommentService {
    void likeComment(Long commentId);

    void unlikeComment(Long commentId);

    void createComment(CommentSaveRequest request);

    void deleteComment(Long commentId);
}
