package com.cybzacg.blogbackend.module.content.friendlink.convert;

import com.cybzacg.blogbackend.domain.content.BlogFriendLink;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkSaveRequest;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkVO;
import com.cybzacg.blogbackend.module.content.friendlink.model.publics.PublicFriendLinkVO;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", imports = StrUtils.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FriendLinkModelConvert {
    FriendLinkVO toVO(BlogFriendLink link);

    List<PublicFriendLinkVO> toPublicVOList(List<BlogFriendLink> links);

    @Mapping(target = "name", expression = "java(StrUtils.trim(request.getName()))")
    @Mapping(target = "url", expression = "java(StrUtils.trim(request.getUrl()))")
    @Mapping(target = "iconUrl", expression = "java(StrUtils.normalize(request.getIconUrl()))")
    @Mapping(target = "description", expression = "java(StrUtils.normalize(request.getDescription()))")
    BlogFriendLink toEntity(FriendLinkSaveRequest request);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(FriendLinkSaveRequest request, @MappingTarget BlogFriendLink link);
}
