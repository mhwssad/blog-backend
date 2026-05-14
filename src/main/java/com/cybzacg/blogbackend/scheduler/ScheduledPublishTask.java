package com.cybzacg.blogbackend.scheduler;

import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleRepository;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSourceTypeEnum;
import com.cybzacg.blogbackend.enums.ai.ContentChangeAction;
import com.cybzacg.blogbackend.module.ai.event.ContentChangeEvent;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章定时发布任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledPublishTask {
    private final BlogArticleRepository blogArticleRepository;
    private final ArticleStatusMachine articleStatusMachine;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 查询并发布已到定时发布时间的文章。
     *
     * <p>扫描状态为待定时发布且计划发布时间已到的文章，执行状态流转并发布内容变更事件。
     * 每轮最多处理 100 篇，防止单次查询数据量过大。</p>
     *
     * @see ArticleStatusMachine#isAwaitingScheduledPublish(BlogArticle, LocalDateTime)
     */
    @Scheduled(cron = "${article.scheduled-publish-cron:0 * * * * *}")
    public void publishReadyArticles() {
        LocalDateTime now = LocalDateTime.now();
        // 按发布时间排序，限制单批次处理量，避免长时间占用调度窗口
        List<BlogArticle> articles = blogArticleRepository.listReadyForScheduledPublish(now, 100);
        if (articles.isEmpty()) {
            return;
        }

        int publishedCount = 0;
        for (BlogArticle article : articles) {
            try {
                // 双重校验：数据库中可能已有其他节点抢先处理，需确认仍处于待发布状态
                if (!articleStatusMachine.isAwaitingScheduledPublish(article, now)) {
                    continue;
                }
                // 状态流转：待定时发布(3) -> 已发布(1)，并清除定时发布时间字段
                article.setStatus(1);
                article.setPublishTime(article.getScheduledPublishTime());
                article.setScheduledPublishTime(null);
                blogArticleRepository.updateById(article);
                // 发布内容变更事件，触发 AI 知识库索引更新
                eventPublisher.publishEvent(new ContentChangeEvent(
                        AiKnowledgeSourceTypeEnum.PUBLIC_ARTICLE.getCode(),
                        article.getId(), ContentChangeAction.PUBLISH, article.getAuthorId()));
                publishedCount++;
            } catch (Exception ex) {
                log.error("文章定时发布失败，articleId={}", article.getId(), ex);
            }
        }

        if (publishedCount > 0) {
            log.info("文章定时发布完成，本次处理文章数: {}", publishedCount);
        }
    }
}
