package com.cybzacg.blogbackend.module.follow.convert;

import com.cybzacg.blogbackend.domain.follow.SysUserFollow;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminRelationVO;
import com.cybzacg.blogbackend.module.follow.model.data.FollowAdminRelationItem;
import com.cybzacg.blogbackend.module.follow.model.data.FollowRelationUserItem;
import com.cybzacg.blogbackend.module.follow.model.data.PublicFollowUserItem;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowUserVO;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowRemarkUpdateRequest;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowSpecialUpdateRequest;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowUserVO;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

/**
 * 关注关系模型转换器。
 *
 * <p>负责收口关注实体构建、更新请求映射和分页列表视图转换。
 */
@Mapper(componentModel = "spring", imports = StrUtils.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FollowModelConvert {
    @Mapping(target = "followerId", source = "userId")
    @Mapping(target = "followingId", source = "targetUserId")
    @Mapping(target = "followStatus", constant = "1")
    @Mapping(target = "isSpecialFollow", constant = "0")
    @Mapping(target = "source", constant = "manual")
    SysUserFollow toNewFollow(Long userId, Long targetUserId, LocalDateTime followTime);

    @Mapping(target = "isSpecialFollow", source = "specialFollow")
    void updateSpecialFollow(UserFollowSpecialUpdateRequest request, @MappingTarget SysUserFollow relation);

    @Mapping(target = "remark", expression = "java(StrUtils.trimToNull(request.getRemark()))")
    void updateRemark(UserFollowRemarkUpdateRequest request, @MappingTarget SysUserFollow relation);

    UserFollowUserVO toUserFollowUserVO(FollowRelationUserItem item);

    FollowAdminRelationVO toFollowAdminRelationVO(FollowAdminRelationItem item);

    PublicFollowUserVO toPublicFollowUserVO(PublicFollowUserItem item);
}
