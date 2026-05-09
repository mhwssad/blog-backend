package com.cybzacg.blogbackend.module.follow.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.follow.model.user.*;

/**
 * 用户关注关系服务接口。
 *
 * <p>定义用户侧关注、取关、列表查询、互关判断和关注资料维护等业务能力。
 */
public interface UserFollowService {
    /**
     * 创建或恢复一条单向关注关系；若已关注则保持幂等。
     *
     * @param targetUserId 被关注用户ID
     */
    void followUser(Long targetUserId);

    /**
     * 将已存在的关注关系标记为取关；重复取关时直接忽略。
     *
     * @param targetUserId 被取消关注的用户ID
     */
    void unfollowUser(Long targetUserId);

    /**
     * 分页查询当前用户的关注列表，支持按特别关注筛选。
     *
     * @param query 分页查询条件，specialOnly 为 null 时不筛选
     * @return 关注用户列表
     */
    PageResult<UserFollowUserVO> pageMyFollows(UserFollowPageQuery query);

    /**
     * 分页查询当前用户的粉丝列表。
     *
     * @param query 分页查询条件
     * @return 粉丝用户列表
     */
    PageResult<UserFollowUserVO> pageMyFans(UserFanPageQuery query);

    /**
     * 查询当前用户与目标用户的互关状态。
     *
     * @param targetUserId 目标用户ID
     * @return 包含是否关注、是否被关注及是否互关的状态视图
     */
    UserFollowMutualVO getMutualFollowStatus(Long targetUserId);

    /**
     * 查询当前用户的关注数与粉丝数，结果默认缓存 5 分钟。
     *
     * @return 关注数与粉丝数聚合视图
     */
    UserFollowCountVO getMyFollowCount();

    /**
     * 更新我对目标用户的特别关注状态，仅允许操作有效关注关系。
     *
     * @param targetUserId 目标用户ID
     * @param request 特别关注更新请求，包含 specialFollow 状态（1=开启，0=关闭）
     */
    void updateSpecialFollow(Long targetUserId, UserFollowSpecialUpdateRequest request);

    /**
     * 更新我对目标用户的备注；空白备注会被归一化为清空。
     *
     * @param targetUserId 目标用户ID
     * @param request 备注更新请求，包含 remark 内容
     */
    void updateRemark(Long targetUserId, UserFollowRemarkUpdateRequest request);
}
