package com.cybzacg.blogbackend.module.forum.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminVO;

/**
 * 论坛回复后台治理服务。
 */
public interface ForumReplyAdminService {
    PageResult<ForumReplyAdminVO> pageReplies(ForumReplyAdminPageQuery query);

    void hideReply(Long id, Long operatorId, String ip, String ua);

    void restoreReply(Long id, Long operatorId, String ip, String ua);

    void deleteReply(Long id, Long operatorId, String ip, String ua);
}
