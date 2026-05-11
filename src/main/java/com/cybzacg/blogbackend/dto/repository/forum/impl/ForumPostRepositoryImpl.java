package com.cybzacg.blogbackend.dto.repository.forum.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.forum.ForumPost;
import com.cybzacg.blogbackend.dto.mapper.forum.ForumPostMapper;
import com.cybzacg.blogbackend.dto.repository.forum.ForumPostRepository;
import com.cybzacg.blogbackend.enums.forum.ForumPostStatusEnum;
import com.cybzacg.blogbackend.enums.forum.ForumVisibilityScopeEnum;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.model.publics.ForumPostPageQuery;
import com.cybzacg.blogbackend.module.forum.model.user.UserForumPostPageQuery;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 论坛帖子 Repository 实现。
 */
@Repository
public class ForumPostRepositoryImpl extends ServiceImpl<ForumPostMapper, ForumPost>
        implements ForumPostRepository {
    @Override
    public Page<ForumPost> pagePublicPosts(ForumPostPageQuery query, boolean loginUser, Collection<Long> visibleSectionIds) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<ForumPost>()
                .eq(ForumPost::getStatus, ForumPostStatusEnum.PUBLISHED.getValue())
                .le(ForumPost::getVisibilityScope, loginUser
                        ? ForumVisibilityScopeEnum.LOGIN_ONLY.getValue()
                        : ForumVisibilityScopeEnum.PUBLIC.getValue())
                .in(visibleSectionIds != null && !visibleSectionIds.isEmpty(), ForumPost::getSectionId, visibleSectionIds)
                .eq(query.getSectionId() != null, ForumPost::getSectionId, query.getSectionId())
                .eq(query.getAuthorId() != null, ForumPost::getAuthorId, query.getAuthorId())
                .ge(query.getCreatedAtStart() != null, ForumPost::getCreatedAt, query.getCreatedAtStart())
                .le(query.getCreatedAtEnd() != null, ForumPost::getCreatedAt, query.getCreatedAtEnd())
                .and(StrUtils.hasText(query.getKeyword()), w -> w.like(ForumPost::getTitle, query.getKeyword())
                        .or()
                        .like(ForumPost::getContent, query.getKeyword()));
        applyPublicSort(wrapper, query.getSort());
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    @Override
    public Page<ForumPost> pageUserPosts(Long authorId, UserForumPostPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<ForumPost>()
                .eq(ForumPost::getAuthorId, authorId)
                .eq(query.getSectionId() != null, ForumPost::getSectionId, query.getSectionId())
                .eq(query.getStatus() != null, ForumPost::getStatus, query.getStatus())
                .and(StrUtils.hasText(query.getKeyword()), w -> w.like(ForumPost::getTitle, query.getKeyword())
                        .or()
                        .like(ForumPost::getContent, query.getKeyword()))
                .orderByDesc(ForumPost::getUpdatedAt)
                .orderByDesc(ForumPost::getId));
    }

    @Override
    public Page<ForumPost> pageAdminPosts(ForumPostAdminPageQuery query) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<ForumPost>()
                .eq(query.getSectionId() != null, ForumPost::getSectionId, query.getSectionId())
                .eq(query.getAuthorId() != null, ForumPost::getAuthorId, query.getAuthorId())
                .eq(query.getStatus() != null, ForumPost::getStatus, query.getStatus())
                .eq(query.getIsTop() != null, ForumPost::getIsTop, query.getIsTop())
                .eq(query.getIsEssence() != null, ForumPost::getIsEssence, query.getIsEssence())
                .ge(query.getCreatedAtStart() != null, ForumPost::getCreatedAt, query.getCreatedAtStart())
                .le(query.getCreatedAtEnd() != null, ForumPost::getCreatedAt, query.getCreatedAtEnd())
                .and(StrUtils.hasText(query.getKeyword()), w -> w.like(ForumPost::getTitle, query.getKeyword())
                        .or()
                        .like(ForumPost::getContent, query.getKeyword()))
                .orderByDesc(ForumPost::getIsTop)
                .orderByDesc(ForumPost::getIsEssence)
                .orderByDesc(ForumPost::getCreatedAt)
                .orderByDesc(ForumPost::getId);
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    @Override
    public List<ForumPost> listPublicVisibleForRag(int limit) {
        int actualLimit = limit <= 0 ? 1000 : limit;
        return list(publicVisibleRagWrapper()
                .orderByDesc(ForumPost::getUpdatedAt)
                .last("limit " + actualLimit));
    }

    @Override
    public ForumPost findPublicVisibleForRag(Long postId) {
        if (postId == null) {
            return null;
        }
        return getOne(publicVisibleRagWrapper()
                .eq(ForumPost::getId, postId)
                .last("limit 1"), false);
    }

    @Override
    public boolean existsBySectionId(Long sectionId) {
        return count(new LambdaQueryWrapper<ForumPost>()
                .eq(ForumPost::getSectionId, sectionId)) > 0;
    }

    private LambdaQueryWrapper<ForumPost> publicVisibleRagWrapper() {
        return new LambdaQueryWrapper<ForumPost>()
                .eq(ForumPost::getStatus, ForumPostStatusEnum.PUBLISHED.getValue())
                .eq(ForumPost::getVisibilityScope, ForumVisibilityScopeEnum.PUBLIC.getValue());
    }

    private void applyPublicSort(LambdaQueryWrapper<ForumPost> wrapper, String sort) {
        String actualSort = sort == null ? "latest" : sort.trim().toLowerCase();
        if ("hot".equals(actualSort)) {
            wrapper.orderByDesc(ForumPost::getIsTop)
                    .orderByDesc(ForumPost::getIsEssence)
                    .orderByDesc(ForumPost::getViewCount)
                    .orderByDesc(ForumPost::getLikeCount)
                    .orderByDesc(ForumPost::getReplyCount)
                    .orderByDesc(ForumPost::getId);
            return;
        }
        wrapper.orderByDesc(ForumPost::getIsTop)
                .orderByDesc(ForumPost::getIsEssence)
                .orderByDesc(ForumPost::getPublishedAt)
                .orderByDesc(ForumPost::getId);
    }

    @Override
    public void incrementLikeCount(Long id, int delta) {
        baseMapper.incrementLikeCount(id, delta);
    }

    @Override
    public void incrementReplyCount(Long id, int delta) {
        baseMapper.incrementReplyCount(id, delta);
    }

    @Override
    public void incrementCollectCount(Long id, int delta) {
        baseMapper.incrementCollectCount(id, delta);
    }

    @Override
    public void incrementShareCount(Long id, int delta) {
        baseMapper.incrementShareCount(id, delta);
    }

    @Override
    public void incrementViewCount(Long id, int delta) {
        baseMapper.incrementViewCount(id, delta);
    }

    @Override
    public void softDeleteById(Long id) {
        lambdaUpdate()
                .eq(ForumPost::getId, id)
                .set(ForumPost::getStatus, ForumPostStatusEnum.DELETED.getValue())
                .update();
    }

    @Override
    public void updateStatusById(Long id, Integer status) {
        lambdaUpdate()
                .eq(ForumPost::getId, id)
                .set(ForumPost::getStatus, status)
                .update();
    }

    @Override
    public void updateTopById(Long id, Integer isTop) {
        lambdaUpdate()
                .eq(ForumPost::getId, id)
                .set(ForumPost::getIsTop, isTop)
                .update();
    }

    @Override
    public void updateEssenceById(Long id, Integer isEssence) {
        lambdaUpdate()
                .eq(ForumPost::getId, id)
                .set(ForumPost::getIsEssence, isEssence)
                .update();
    }
}
