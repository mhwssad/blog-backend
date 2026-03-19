package com.cybzacg.blogbackend.module.content.convert;

import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.domain.SysUserFootprint;
import com.cybzacg.blogbackend.module.content.model.admin.CategoryAdminVO;
import com.cybzacg.blogbackend.module.content.model.admin.CategoryTreeVO;
import com.cybzacg.blogbackend.module.content.model.admin.CommentVO;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintVO;
import com.cybzacg.blogbackend.module.content.model.admin.InteractionVO;
import com.cybzacg.blogbackend.module.content.model.admin.TagVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCommentVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicTagVO;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.model.user.UserFootprintVO;
import com.cybzacg.blogbackend.utils.JsonUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ContentModelMapper {
    CategoryAdminVO toCategoryAdminVO(SysCategory category);

    CategoryTreeVO toCategoryTreeVO(SysCategory category);

    PublicCategoryTreeVO toPublicCategoryTreeVO(SysCategory category);

    TagVO toTagVO(SysTag tag);

    PublicTagVO toPublicTagVO(SysTag tag);

    @Mapping(target = "images", expression = "java(toStringList(comment.getImages()))")
    CommentVO toCommentVO(SysComment comment);

    @Mapping(target = "images", expression = "java(toStringList(comment.getImages()))")
    PublicCommentVO toPublicCommentVO(SysComment comment);

    com.cybzacg.blogbackend.module.content.model.admin.CollectionVO toAdminCollectionVO(SysCollection collection);

    com.cybzacg.blogbackend.module.content.model.user.CollectionVO toUserCollectionVO(SysCollection collection);

    CollectionFolderVO toCollectionFolderVO(SysCollectionFolder folder);

    InteractionVO toInteractionVO(SysInteraction interaction);

    @Mapping(target = "targetTitle", source = "title")
    @Mapping(target = "targetUrl", source = "url")
    FootprintVO toFootprintVO(SysUserFootprint footprint);

    @Mapping(target = "targetTitle", source = "title")
    @Mapping(target = "targetUrl", source = "url")
    UserFootprintVO toUserFootprintVO(SysUserFootprint footprint);

    default List<String> toStringList(String json) {
        List<String> images = JsonUtils.fromJsonToList(json, String.class);
        return images == null ? new ArrayList<>() : images;
    }

    @AfterMapping
    default void initCategoryChildren(@MappingTarget CategoryTreeVO categoryTreeVO) {
        if (categoryTreeVO.getChildren() == null) {
            categoryTreeVO.setChildren(new ArrayList<>());
        }
    }

    @AfterMapping
    default void initPublicCategoryChildren(@MappingTarget PublicCategoryTreeVO categoryTreeVO) {
        if (categoryTreeVO.getChildren() == null) {
            categoryTreeVO.setChildren(new ArrayList<>());
        }
    }

    @AfterMapping
    default void initCommentChildren(@MappingTarget CommentVO commentVO) {
        if (commentVO.getChildren() == null) {
            commentVO.setChildren(new ArrayList<>());
        }
    }

    @AfterMapping
    default void initPublicCommentChildren(@MappingTarget PublicCommentVO commentVO) {
        if (commentVO.getChildren() == null) {
            commentVO.setChildren(new ArrayList<>());
        }
    }
}
