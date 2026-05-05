package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.domain.ai.AiKnowledgeEntry;
import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.forum.ForumPost;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeEntryStatusEnum;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSourceTypeEnum;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeSourceExtractor;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.forum.repository.ForumPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * AI 知识源抽取服务实现。
 */
@Service
@RequiredArgsConstructor
public class AiKnowledgeSourceExtractorImpl implements AiKnowledgeSourceExtractor {
    private static final int DEFAULT_EXTRACT_LIMIT = 1000;

    private final BlogArticleRepository blogArticleRepository;
    private final ForumPostRepository forumPostRepository;
    private final SysUserRepository sysUserRepository;

    @Override
    public List<AiKnowledgeEntry> extractAll(String sourceType) {
        AiKnowledgeSourceTypeEnum type = AiKnowledgeSourceTypeEnum.fromCode(sourceType);
        if (type == AiKnowledgeSourceTypeEnum.PUBLIC_ARTICLE) {
            return blogArticleRepository.listPublicVisibleForRag(DEFAULT_EXTRACT_LIMIT).stream()
                    .map(this::fromArticle)
                    .toList();
        }
        if (type == AiKnowledgeSourceTypeEnum.FORUM_POST) {
            return forumPostRepository.listPublicVisibleForRag(DEFAULT_EXTRACT_LIMIT).stream()
                    .map(this::fromForumPost)
                    .toList();
        }
        if (type == AiKnowledgeSourceTypeEnum.AUTHOR_PROFILE) {
            return sysUserRepository.listPublicProfilesForRag(DEFAULT_EXTRACT_LIMIT).stream()
                    .map(this::fromUserProfile)
                    .filter(entry -> StringUtils.hasText(entry.getContentSnapshot()))
                    .toList();
        }
        return List.of();
    }

    @Override
    public AiKnowledgeEntry extractOne(String sourceType, Long sourceId) {
        AiKnowledgeSourceTypeEnum type = AiKnowledgeSourceTypeEnum.fromCode(sourceType);
        if (type == AiKnowledgeSourceTypeEnum.PUBLIC_ARTICLE) {
            BlogArticle article = blogArticleRepository.findPublicVisibleForRag(sourceId);
            return article == null ? null : fromArticle(article);
        }
        if (type == AiKnowledgeSourceTypeEnum.FORUM_POST) {
            ForumPost post = forumPostRepository.findPublicVisibleForRag(sourceId);
            return post == null ? null : fromForumPost(post);
        }
        if (type == AiKnowledgeSourceTypeEnum.AUTHOR_PROFILE) {
            SysUser user = sysUserRepository.findPublicProfileForRag(sourceId);
            return user == null ? null : fromUserProfile(user);
        }
        return null;
    }

    private AiKnowledgeEntry fromArticle(BlogArticle article) {
        AiKnowledgeEntry entry = baseEntry(
                AiKnowledgeSourceTypeEnum.PUBLIC_ARTICLE.getCode(),
                article.getId(),
                article.getTitle(),
                article.getAuthorId(),
                "/articles/" + article.getId());
        entry.setSummary(article.getSummary());
        entry.setContentSnapshot(joinText(article.getTitle(), article.getSummary(), article.getContent()));
        entry.setSourceUpdatedAt(article.getUpdatedAt());
        return entry;
    }

    private AiKnowledgeEntry fromForumPost(ForumPost post) {
        AiKnowledgeEntry entry = baseEntry(
                AiKnowledgeSourceTypeEnum.FORUM_POST.getCode(),
                post.getId(),
                post.getTitle(),
                post.getAuthorId(),
                "/forum/posts/" + post.getId());
        entry.setContentSnapshot(joinText(post.getTitle(), null, post.getContent()));
        entry.setSourceUpdatedAt(post.getUpdatedAt());
        return entry;
    }

    private AiKnowledgeEntry fromUserProfile(SysUser user) {
        String title = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
        AiKnowledgeEntry entry = baseEntry(
                AiKnowledgeSourceTypeEnum.AUTHOR_PROFILE.getCode(),
                user.getId(),
                title,
                user.getId(),
                "/users/" + user.getId() + "/author-profile");
        entry.setSummary(user.getBio());
        entry.setContentSnapshot(joinText(title, user.getBio(), user.getWebsite()));
        entry.setSourceUpdatedAt(user.getUpdatedAt());
        return entry;
    }

    private AiKnowledgeEntry baseEntry(String sourceType, Long sourceId, String title, Long authorId, String sourceUrl) {
        AiKnowledgeEntry entry = new AiKnowledgeEntry();
        entry.setSourceType(sourceType);
        entry.setSourceId(sourceId);
        entry.setTitle(StringUtils.hasText(title) ? title : "未命名知识");
        entry.setSourceUrl(sourceUrl);
        entry.setAuthorId(authorId);
        entry.setStatus(AiKnowledgeEntryStatusEnum.ACTIVE.getValue());
        return entry;
    }

    private String joinText(String title, String summary, String content) {
        return List.of(title, summary, content).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }
}
