package com.cybzacg.blogbackend.module.follow.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowPageQuery;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowUserVO;

/**
 * 公开关注关系服务。
 *
 * <p>提供无需登录即可访问的用户级关注列表与粉丝列表查询接口。
 */
public interface PublicFollowService {
    /**
     * 分页查询指定用户的关注列表（公开接口）。
     * 若用户不存在或非正常状态则抛出业务异常。
     *
     * @param userId 指定用户ID
     * @param query 分页查询条件
     * @return 指定用户的关注用户分页列表
     */
    PageResult<PublicFollowUserVO> pageUserFollows(Long userId, PublicFollowPageQuery query);

    /**
     * 分页查询指定用户的粉丝列表（公开接口）。
     * 若用户不存在或非正常状态则抛出业务异常。
     *
     * @param userId 指定用户ID
     * @param query 分页查询条件
     * @return 指定用户的粉丝用户分页列表
     */
    PageResult<PublicFollowUserVO> pageUserFans(Long userId, PublicFollowPageQuery query);
}
