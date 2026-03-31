package com.cybzacg.blogbackend.module.follow.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowPageQuery;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowUserVO;

/**
 * 公开关注关系服务。
 */
public interface PublicFollowService {
    /**
     * 分页查询指定用户的关注列表。
     */
    PageResult<PublicFollowUserVO> pageUserFollows(Long userId, PublicFollowPageQuery query);

    /**
     * 分页查询指定用户的粉丝列表。
     */
    PageResult<PublicFollowUserVO> pageUserFans(Long userId, PublicFollowPageQuery query);
}
