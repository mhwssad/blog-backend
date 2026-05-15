package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeEntry;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.forum.ForumPost;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleRepository;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumPostRepository;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeEntryStatusEnum;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSourceTypeEnum;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeSourceExtractor;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 知识源抽取服务实现。
 *
 * <p>从不同的业务数据源（公开文章、论坛帖子、作者档案）中抽取结构化知识条目，
 * 用于后续的分块和向量化索引。每种数据源有独立的抽取逻辑和内容拼接策略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeSourceExtractorImpl implements AiKnowledgeSourceExtractor {
    private static final int DEFAULT_EXTRACT_LIMIT = 1000;

    private final BlogArticleRepository blogArticleRepository;
    private final ForumPostRepository forumPostRepository;
    private final SysUserRepository sysUserRepository;

    /**
     * 全量抽取指定来源类型的所有可见知识条目。
     *
     * <p>根据来源类型从不同的数据表查询公开可见的记录，转换为统一的知识条目格式。
     * 最多抽取 {@value #DEFAULT_EXTRACT_LIMIT} 条。
     *
     * @param sourceType 来源类型编码，参见 {@link AiKnowledgeSourceTypeEnum}
     * @return 抽取到的知识条目列表，来源类型不匹配时返回空列表
     */
    @Override
    public List<AiKnowledgeEntry> extractAll(String sourceType) {
        AiKnowledgeSourceTypeEnum type = AiKnowledgeSourceTypeEnum.fromCode(sourceType);
        if (type == AiKnowledgeSourceTypeEnum.PUBLIC_ARTICLE) {
            List<AiKnowledgeEntry> entries = blogArticleRepository.listPublicVisibleForRag(DEFAULT_EXTRACT_LIMIT).stream()
                    .map(this::fromArticle)
                    .toList();
            log.debug("全量抽取公开文章知识条目: count={}", entries.size());
            return entries;
        }
        if (type == AiKnowledgeSourceTypeEnum.FORUM_POST) {
            List<AiKnowledgeEntry> entries = forumPostRepository.listPublicVisibleForRag(DEFAULT_EXTRACT_LIMIT).stream()
                    .map(this::fromForumPost)
                    .toList();
            log.debug("全量抽取论坛帖子知识条目: count={}", entries.size());
            return entries;
        }
        if (type == AiKnowledgeSourceTypeEnum.AUTHOR_PROFILE) {
            List<AiKnowledgeEntry> entries = sysUserRepository.listPublicProfilesForRag(DEFAULT_EXTRACT_LIMIT).stream()
                    .map(this::fromUserProfile)
                    .filter(entry -> StrUtils.hasText(entry.getContentSnapshot()))
                    .toList();
            log.debug("全量抽取作者档案知识条目: count={}", entries.size());
            return entries;
        }
        return List.of();
    }

    /**
     * 抽取指定来源类型的单条知识条目。
     *
     * @param sourceType 来源类型编码
     * @param sourceId   来源数据的主键 ID
     * @return 知识条目，来源数据不存在或不公开时返回 null
     */
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

    /**
     * 将博客文章实体转换为知识条目，拼接标题、摘要和正文。
     */
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

    /**
     * 将论坛帖子实体转换为知识条目，拼接标题和正文。
     */
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

    /**
     * 将用户档案转换为知识条目，拼接昵称、简介和网站。无内容的用户档案会被过滤。
     */
    private AiKnowledgeEntry fromUserProfile(SysUser user) {
        String title = StrUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
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

    /**
     * 构建知识条目的基础字段（来源类型、来源ID、标题、作者、URL、状态）。
     */
    private AiKnowledgeEntry baseEntry(String sourceType, Long sourceId, String title, Long authorId, String sourceUrl) {
        AiKnowledgeEntry entry = new AiKnowledgeEntry();
        entry.setSourceType(sourceType);
        entry.setSourceId(sourceId);
        entry.setTitle(StrUtils.hasText(title) ? title : "未命名知识");
        entry.setSourceUrl(sourceUrl);
        entry.setAuthorId(authorId);
        entry.setStatus(AiKnowledgeEntryStatusEnum.ACTIVE.getValue());
        return entry;
    }

    /**
     * 将多段文本按顺序拼接，过滤空值，段间以双换行分隔。
     */
    private String joinText(String title, String summary, String content) {
        return List.of(title, summary, content).stream()
                .filter(StrUtils::hasText)
                .map(String::trim)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }
}
