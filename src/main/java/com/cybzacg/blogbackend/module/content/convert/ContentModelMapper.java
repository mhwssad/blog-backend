package com.cybzacg.blogbackend.module.content.convert;

import com.cybzacg.blogbackend.domain.*;
import com.cybzacg.blogbackend.module.content.model.admin.*;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCommentVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicTagVO;
import com.cybzacg.blogbackend.module.content.model.user.*;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 内容模块对象转换器，涵盖分类、标签、评论、收藏、互动及足迹的映射。
 */
@Mapper(componentModel = "spring", imports = StrUtils.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", source = "parentId")
    @Mapping(target = "name", expression = "java(StrUtils.trim(request.getName()))")
    @Mapping(target = "code", expression = "java(StrUtils.trim(request.getCode()))")
    @Mapping(target = "type", expression = "java(StrUtils.trim(request.getType()))")
    @Mapping(target = "ancestors", ignore = true)
    @Mapping(target = "level", ignore = true)
    @Mapping(target = "sortOrder", source = "sortOrder")
    @Mapping(target = "icon", expression = "java(StrUtils.normalize(request.getIcon()))")
    @Mapping(target = "description", expression = "java(StrUtils.normalize(request.getDescription()))")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SysCategory toCategory(CategorySaveRequest request);

    @InheritConfiguration(name = "toCategory")
    void updateCategory(CategorySaveRequest request, @MappingTarget SysCategory category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", expression = "java(StrUtils.trim(request.getName()))")
    @Mapping(target = "color", expression = "java(StrUtils.normalize(request.getColor()))")
    @Mapping(target = "createdAt", ignore = true)
    SysTag toTag(TagSaveRequest request);

    @InheritConfiguration(name = "toTag")
    void updateTag(TagSaveRequest request, @MappingTarget SysTag tag);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "folderName", expression = "java(StrUtils.trim(request.getFolderName()))")
    @Mapping(target = "folderType", ignore = true)
    @Mapping(target = "description", expression = "java(StrUtils.normalize(request.getDescription()))")
    @Mapping(target = "isPublic", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "collectionCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SysCollectionFolder toCollectionFolder(CollectionFolderSaveRequest request);

    @InheritConfiguration(name = "toCollectionFolder")
    void updateCollectionFolder(CollectionFolderSaveRequest request, @MappingTarget SysCollectionFolder folder);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "folderName", constant = "默认收藏夹")
    @Mapping(target = "folderType", source = "folderType")
    @Mapping(target = "description", constant = "系统自动创建的默认收藏夹")
    @Mapping(target = "isPublic", constant = "0")
    @Mapping(target = "isDefault", constant = "1")
    @Mapping(target = "sortOrder", constant = "0")
    @Mapping(target = "collectionCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SysCollectionFolder toDefaultCollectionFolder(Long userId, String folderType);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "folderId", source = "folderId")
    @Mapping(target = "targetId", source = "article.id")
    @Mapping(target = "targetType", constant = "article")
    @Mapping(target = "remark", expression = "java(StrUtils.normalize(request.getRemark()))")
    @Mapping(target = "targetTitle", source = "article.title")
    @Mapping(target = "targetUrl", expression = "java(article == null ? null : \"/article/\" + article.getId())")
    @Mapping(target = "createdAt", ignore = true)
    SysCollection toCollection(CollectionSaveRequest request, Long userId, Long folderId, BlogArticle article);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "targetId", source = "targetId")
    @Mapping(target = "targetType", expression = "java(StrUtils.trim(request.getTargetType()))")
    @Mapping(target = "content", expression = "java(StrUtils.trim(request.getContent()))")
    @Mapping(target = "images", expression = "java(toJson(request.getImages()))")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "rootId", source = "rootId")
    @Mapping(target = "parentId", source = "parentId")
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SysComment toComment(CommentSaveRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "targetId", source = "targetId")
    @Mapping(target = "targetType", source = "targetType")
    @Mapping(target = "actionType", source = "actionType")
    @Mapping(target = "createdAt", ignore = true)
    SysInteraction toInteraction(Long userId, Long targetId, String targetType, String actionType);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "targetId", source = "article.id")
    @Mapping(target = "targetType", constant = "article")
    @Mapping(target = "title", source = "article.title")
    @Mapping(target = "url", expression = "java(article == null ? null : \"/article/\" + article.getId())")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "userAgent", source = "userAgent")
    @Mapping(target = "visitedAt", source = "visitedAt")
    SysUserFootprint toArticleFootprint(Long userId, BlogArticle article, String ipAddress, String userAgent, LocalDateTime visitedAt);

    default List<String> toStringList(String json) {
        List<String> images = JsonUtils.fromJsonToList(json, String.class);
        return images == null ? new ArrayList<>() : images;
    }

    default String toJson(List<String> images) {
        return images == null || images.isEmpty() ? null : JsonUtils.toJson(images);
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
