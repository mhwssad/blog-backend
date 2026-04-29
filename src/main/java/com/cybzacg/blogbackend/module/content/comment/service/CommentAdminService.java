package com.cybzacg.blogbackend.module.content.comment.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.content.comment.model.admin.CommentPageQuery;
import com.cybzacg.blogbackend.module.content.comment.model.admin.CommentVO;

/**
 * 评论后台管理服务接口。
 *
 * <p>定义评论后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface CommentAdminService {
    PageResult<CommentVO> pageComments(CommentPageQuery query);

    CommentVO getComment(Long id);

    void updateStatus(Long id, Integer status);

    void deleteComment(Long id);
}
