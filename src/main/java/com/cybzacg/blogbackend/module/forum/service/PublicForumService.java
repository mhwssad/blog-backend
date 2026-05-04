package com.cybzacg.blogbackend.module.forum.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.forum.model.publics.*;

import java.util.List;

/**
 * 公开论坛查询服务。
 */
public interface PublicForumService {
    List<ForumSectionVO> listSections();

    PageResult<PublicForumPostVO> pagePosts(ForumPostPageQuery query);

    PublicForumPostDetailVO getPost(Long id);

    PageResult<PublicForumReplyVO> pageReplies(Long postId, Long current, Long size);
}
