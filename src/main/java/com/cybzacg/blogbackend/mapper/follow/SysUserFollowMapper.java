package com.cybzacg.blogbackend.mapper.follow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.follow.SysUserFollow;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminPageQuery;
import com.cybzacg.blogbackend.module.follow.model.data.FollowAdminRelationItem;
import com.cybzacg.blogbackend.module.follow.model.data.FollowRelationUserItem;
import com.cybzacg.blogbackend.module.follow.model.data.PublicFollowUserItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户关注关系 Mapper。
 */
public interface SysUserFollowMapper extends BaseMapper<SysUserFollow> {
    Long countFollowPage(@Param("userId") Long userId, @Param("specialOnly") Boolean specialOnly);

    List<FollowRelationUserItem> selectFollowPage(@Param("userId") Long userId,
                                                  @Param("specialOnly") Boolean specialOnly,
                                                  @Param("offset") Long offset,
                                                  @Param("size") Long size);

    Long countFanPage(@Param("userId") Long userId);

    List<FollowRelationUserItem> selectFanPage(@Param("userId") Long userId,
                                               @Param("offset") Long offset,
                                               @Param("size") Long size);

    Long countActiveFollowing(@Param("userId") Long userId);

    Long countActiveFans(@Param("userId") Long userId);

    Long countActiveRelation(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    Long countPublicFollowPage(@Param("userId") Long userId);

    List<PublicFollowUserItem> selectPublicFollowPage(@Param("userId") Long userId,
                                                      @Param("offset") Long offset,
                                                      @Param("size") Long size);

    Long countPublicFanPage(@Param("userId") Long userId);

    List<PublicFollowUserItem> selectPublicFanPage(@Param("userId") Long userId,
                                                   @Param("offset") Long offset,
                                                   @Param("size") Long size);

    Long countAdminRelationPage(@Param("query") FollowAdminPageQuery query);

    List<FollowAdminRelationItem> selectAdminRelationPage(@Param("query") FollowAdminPageQuery query,
                                                          @Param("offset") Long offset,
                                                          @Param("size") Long size);

    Long countCleanableRelations(@Param("cleanInactive") boolean cleanInactive,
                                 @Param("cleanDeletedUsers") boolean cleanDeletedUsers,
                                 @Param("cleanDisabledUsers") boolean cleanDisabledUsers);

    int deleteCleanableRelations(@Param("cleanInactive") boolean cleanInactive,
                                 @Param("cleanDeletedUsers") boolean cleanDeletedUsers,
                                 @Param("cleanDisabledUsers") boolean cleanDisabledUsers);
}
