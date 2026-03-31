package com.cybzacg.blogbackend.module.follow.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.follow.model.user.UserFanPageQuery;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowCountVO;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowMutualVO;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowPageQuery;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowRemarkUpdateRequest;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowSpecialUpdateRequest;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowUserVO;

/**
 * 用户关注关系服务接口。
 *
 * <p>定义用户侧关注、取关、列表查询、互关判断和关注资料维护等业务能力。
 */
public interface UserFollowService {
    void followUser(Long targetUserId);

    void unfollowUser(Long targetUserId);

    PageResult<UserFollowUserVO> pageMyFollows(UserFollowPageQuery query);

    PageResult<UserFollowUserVO> pageMyFans(UserFanPageQuery query);

    UserFollowMutualVO getMutualFollowStatus(Long targetUserId);

    UserFollowCountVO getMyFollowCount();

    void updateSpecialFollow(Long targetUserId, UserFollowSpecialUpdateRequest request);

    void updateRemark(Long targetUserId, UserFollowRemarkUpdateRequest request);
}
