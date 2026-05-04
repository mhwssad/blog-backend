package com.cybzacg.blogbackend.module.forum.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.forum.ForumPost;
import com.cybzacg.blogbackend.enums.forum.ForumPostStatusEnum;
import com.cybzacg.blogbackend.enums.forum.ForumVisibilityScopeEnum;
import com.cybzacg.blogbackend.mapper.forum.ForumPostMapper;
import com.cybzacg.blogbackend.module.forum.model.publics.ForumPostPageQuery;
import com.cybzacg.blogbackend.module.forum.model.user.UserForumPostPageQuery;
import com.cybzacg.blogbackend.module.forum.repository.ForumPostRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;

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
                .and(StringUtils.hasText(query.getKeyword()), w -> w.like(ForumPost::getTitle, query.getKeyword())
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
                .and(StringUtils.hasText(query.getKeyword()), w -> w.like(ForumPost::getTitle, query.getKeyword())
                        .or()
                        .like(ForumPost::getContent, query.getKeyword()))
                .orderByDesc(ForumPost::getUpdatedAt)
                .orderByDesc(ForumPost::getId));
    }

    @Override
    public boolean existsBySectionId(Long sectionId) {
        return count(new LambdaQueryWrapper<ForumPost>()
                .eq(ForumPost::getSectionId, sectionId)) > 0;
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
}
