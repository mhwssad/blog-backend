package com.cybzacg.blogbackend.module.follow.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysUserFollow;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminPageQuery;
import com.cybzacg.blogbackend.module.follow.model.data.FollowAdminRelationItem;
import com.cybzacg.blogbackend.module.follow.model.data.FollowRelationUserItem;
import com.cybzacg.blogbackend.module.follow.model.data.PublicFollowUserItem;

import java.util.List;

/**
 * 用户关注关系 Repository。
 */
public interface SysUserFollowRepository extends IService<SysUserFollow> {
    /**
     * 根据关注人和被关注人查询单条关系。
     *
     * @param followerId 关注人 ID
     * @param followingId 被关注人 ID
     * @return 关注关系
     */
    SysUserFollow findByFollowerAndFollowing(Long followerId, Long followingId);

    /**
     * 统计用户关注列表总数。
     *
     * @param userId 用户 ID
     * @param specialOnly 是否仅特别关注
     * @return 总数
     */
    Long countFollowPage(Long userId, Boolean specialOnly);

    /**
     * 查询用户关注列表。
     *
     * @param userId 用户 ID
     * @param specialOnly 是否仅特别关注
     * @param offset 偏移量
     * @param size 页大小
     * @return 关注列表
     */
    List<FollowRelationUserItem> selectFollowPage(Long userId, Boolean specialOnly, Long offset, Long size);

    /**
     * 统计用户粉丝列表总数。
     *
     * @param userId 用户 ID
     * @return 总数
     */
    Long countFanPage(Long userId);

    /**
     * 查询用户粉丝列表。
     *
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param size 页大小
     * @return 粉丝列表
     */
    List<FollowRelationUserItem> selectFanPage(Long userId, Long offset, Long size);

    /**
     * 统计一对用户之间是否存在有效关注关系。
     *
     * @param followerId 关注人 ID
     * @param followingId 被关注人 ID
     * @return 关系数
     */
    Long countActiveRelation(Long followerId, Long followingId);

    /**
     * 统计用户有效关注数。
     *
     * @param userId 用户 ID
     * @return 关注数
     */
    Long countActiveFollowing(Long userId);

    /**
     * 统计用户有效粉丝数。
     *
     * @param userId 用户 ID
     * @return 粉丝数
     */
    Long countActiveFans(Long userId);

    /**
     * 统计公开关注列表总数。
     *
     * @param userId 用户 ID
     * @return 总数
     */
    Long countPublicFollowPage(Long userId);

    /**
     * 查询公开关注列表。
     *
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param size 页大小
     * @return 公开关注列表
     */
    List<PublicFollowUserItem> selectPublicFollowPage(Long userId, Long offset, Long size);

    /**
     * 统计公开粉丝列表总数。
     *
     * @param userId 用户 ID
     * @return 总数
     */
    Long countPublicFanPage(Long userId);

    /**
     * 查询公开粉丝列表。
     *
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param size 页大小
     * @return 公开粉丝列表
     */
    List<PublicFollowUserItem> selectPublicFanPage(Long userId, Long offset, Long size);

    /**
     * 统计后台关注关系分页总数。
     *
     * @param query 查询条件
     * @return 总数
     */
    Long countAdminRelationPage(FollowAdminPageQuery query);

    /**
     * 查询后台关注关系分页数据。
     *
     * @param query 查询条件
     * @param offset 偏移量
     * @param size 页大小
     * @return 分页数据
     */
    List<FollowAdminRelationItem> selectAdminRelationPage(FollowAdminPageQuery query, Long offset, Long size);

    /**
     * 统计可清理的无效关系数。
     *
     * @param cleanInactive 是否清理已取关
     * @param cleanDeletedUsers 是否清理已删除用户关系
     * @param cleanDisabledUsers 是否清理已禁用用户关系
     * @return 可清理数量
     */
    Long countCleanableRelations(boolean cleanInactive, boolean cleanDeletedUsers, boolean cleanDisabledUsers);

    /**
     * 删除符合条件的无效关系。
     *
     * @param cleanInactive 是否清理已取关
     * @param cleanDeletedUsers 是否清理已删除用户关系
     * @param cleanDisabledUsers 是否清理已禁用用户关系
     * @return 删除行数
     */
    int deleteCleanableRelations(boolean cleanInactive, boolean cleanDeletedUsers, boolean cleanDisabledUsers);
}
