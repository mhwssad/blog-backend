package com.cybzacg.blogbackend.module.forum.convert;

import com.cybzacg.blogbackend.domain.forum.ForumPost;
import com.cybzacg.blogbackend.domain.forum.ForumReply;
import com.cybzacg.blogbackend.domain.forum.ForumSection;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminDetailVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionAdminVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionSaveRequest;
import com.cybzacg.blogbackend.module.forum.model.publics.ForumSectionVO;
import com.cybzacg.blogbackend.module.forum.model.publics.PublicForumPostDetailVO;
import com.cybzacg.blogbackend.module.forum.model.publics.PublicForumPostVO;
import com.cybzacg.blogbackend.module.forum.model.publics.PublicForumReplyVO;
import com.cybzacg.blogbackend.module.forum.model.user.ForumPostSaveRequest;
import com.cybzacg.blogbackend.module.forum.model.user.UserForumPostDetailVO;
import com.cybzacg.blogbackend.module.forum.model.user.UserForumPostVO;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.*;

/**
 * 论坛模块对象转换器。
 */
@Mapper(
        componentModel = "spring",
        imports = StrUtils.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ForumModelConvert {
    ForumSectionVO toSectionVO(ForumSection section);

    ForumSectionAdminVO toSectionAdminVO(ForumSection section);

    @Mapping(target = "name", expression = "java(StrUtils.trim(request.getName()))")
    @Mapping(target = "description", expression = "java(StrUtils.trimToNull(request.getDescription()))")
    ForumSection toSection(ForumSectionSaveRequest request);

    @InheritConfiguration(name = "toSection")
    void updateSection(ForumSectionSaveRequest request, @MappingTarget ForumSection section);

    PublicForumPostVO toPublicPostVO(ForumPost post);

    PublicForumPostDetailVO toPublicPostDetailVO(ForumPost post);

    UserForumPostVO toUserPostVO(ForumPost post);

    UserForumPostDetailVO toUserPostDetailVO(ForumPost post);

    PublicForumReplyVO toPublicReplyVO(ForumReply reply);

    ForumPostAdminVO toPostAdminVO(ForumPost post);

    ForumPostAdminDetailVO toPostAdminDetailVO(ForumPost post);

    ForumReplyAdminVO toReplyAdminVO(ForumReply reply);

    @Mapping(target = "title", expression = "java(StrUtils.trim(request.getTitle()))")
    @Mapping(target = "content", expression = "java(StrUtils.trim(request.getContent()))")
    ForumPost toPost(ForumPostSaveRequest request);

    @InheritConfiguration(name = "toPost")
    void updatePost(ForumPostSaveRequest request, @MappingTarget ForumPost post);
}
