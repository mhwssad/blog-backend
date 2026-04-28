package com.cybzacg.blogbackend.module.article.task;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Scheduled(cron = "${article.scheduled-publish-cron:0 * * * * *}")
    public void publishReadyArticles() {
        LocalDateTime now = LocalDateTime.now();
        List<BlogArticle> articles = blogArticleRepository.listReadyForScheduledPublish(now, 100);
        if (articles.isEmpty()) {
            return;
        }

        int publishedCount = 0;
        for (BlogArticle article : articles) {
            try {
                if (!articleStatusMachine.isAwaitingScheduledPublish(article, now)) {
                    continue;
                }
                article.setStatus(1);
                article.setPublishTime(article.getScheduledPublishTime());
                article.setScheduledPublishTime(null);
                blogArticleRepository.updateById(article);
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
