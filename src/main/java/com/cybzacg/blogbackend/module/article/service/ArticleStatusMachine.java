package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.enums.article.ArticleReviewStatusEnum;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.CollectionUtils;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 文章状态机。
 */
@Component
public class ArticleStatusMachine {
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_OFFLINE = 2;
    private static final int ACCESS_LEVEL_PUBLIC = 0;
    private static final int ACCESS_LEVEL_WHITELIST = 4;

    public Integer normalizeStatus(Integer status) {
        int actualStatus = CollectionUtils.defaultInt(status);
        ExceptionThrowerCore.throwBusinessIf(
                actualStatus < STATUS_DRAFT || actualStatus > STATUS_OFFLINE,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文章状态非法");
        return actualStatus;
    }

    public Integer normalizeReviewStatus(Integer reviewStatus) {
        int actualReviewStatus = reviewStatus == null
                ? ArticleReviewStatusEnum.NOT_SUBMITTED.getValue()
                : reviewStatus;
        ExceptionThrowerCore.throwBusinessIf(
                !ArticleReviewStatusEnum.contains(actualReviewStatus),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文章审核状态非法");
        return actualReviewStatus;
    }

    public Integer normalizeVisibilityScope(Integer visibilityScope) {
        int actualScope = visibilityScope == null
                ? ArticleVisibilityScopeEnum.PUBLIC.getValue()
                : visibilityScope;
        ExceptionThrowerCore.throwBusinessIf(
                !ArticleVisibilityScopeEnum.contains(actualScope),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文章可见范围非法");
        return actualScope;
    }

    public void validateSaveState(Integer status,
                                  Integer reviewStatus,
                                  Integer visibilityScope,
                                  Integer accessLevel,
                                  LocalDateTime scheduledPublishTime) {
        int actualStatus = normalizeStatus(status);
        int actualReviewStatus = normalizeReviewStatus(reviewStatus);
        normalizeVisibilityScope(visibilityScope);
        int actualAccessLevel = CollectionUtils.defaultInt(accessLevel);
        ExceptionThrowerCore.throwBusinessIf(
                actualAccessLevel < ACCESS_LEVEL_PUBLIC || actualAccessLevel > ACCESS_LEVEL_WHITELIST,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文章访问级别非法");
        if (scheduledPublishTime != null) {
            ExceptionThrowerCore.throwBusinessIf(
                    actualStatus == STATUS_OFFLINE,
                    ResultErrorCode.ILLEGAL_ARGUMENT,
                    "下架文章不能设置定时发布");
            ExceptionThrowerCore.throwBusinessIf(
                    actualReviewStatus == ArticleReviewStatusEnum.REVIEWING.getValue()
                            || actualReviewStatus == ArticleReviewStatusEnum.REJECTED.getValue(),
                    ResultErrorCode.ILLEGAL_ARGUMENT,
                    "当前审核状态不允许定时发布");
        }
    }

    public Integer resolveStatusForSave(Integer requestedStatus, LocalDateTime scheduledPublishTime) {
        int actualStatus = normalizeStatus(requestedStatus);
        if (actualStatus == STATUS_PUBLISHED && isScheduledForFuture(scheduledPublishTime, LocalDateTime.now())) {
            return STATUS_DRAFT;
        }
        return actualStatus;
    }

    public LocalDateTime resolvePublishTime(Integer persistedStatus,
                                            LocalDateTime requestedPublishTime,
                                            LocalDateTime existingPublishTime,
                                            LocalDateTime scheduledPublishTime) {
        int actualStatus = normalizeStatus(persistedStatus);
        if (actualStatus == STATUS_PUBLISHED) {
            return requestedPublishTime != null
                    ? requestedPublishTime
                    : (existingPublishTime != null ? existingPublishTime : LocalDateTime.now());
        }
        if (isScheduledForFuture(scheduledPublishTime, LocalDateTime.now())) {
            return null;
        }
        return requestedPublishTime != null ? requestedPublishTime : existingPublishTime;
    }

    public boolean canShowInPublicList(BlogArticle article) {
        if (article == null) {
            return false;
        }
        return isPublishedForNormalUsers(article, LocalDateTime.now())
                && normalizeVisibilityScope(article.getVisibilityScope()).equals(ArticleVisibilityScopeEnum.PUBLIC.getValue())
                && CollectionUtils.defaultInt(article.getAccessLevel()) == ACCESS_LEVEL_PUBLIC;
    }

    public boolean isPublishedForNormalUsers(BlogArticle article, LocalDateTime now) {
        if (article == null) {
            return false;
        }
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(normalizeStatus(article.getStatus()))) {
            return false;
        }
        Integer reviewStatus = normalizeReviewStatus(article.getReviewStatus());
        if (reviewStatus.equals(ArticleReviewStatusEnum.REVIEWING.getValue())
                || reviewStatus.equals(ArticleReviewStatusEnum.REJECTED.getValue())) {
            return false;
        }
        return !isScheduledForFuture(article.getScheduledPublishTime(), now);
    }

    public boolean canInteract(BlogArticle article) {
        return isPublishedForNormalUsers(article, LocalDateTime.now());
    }

    public boolean isAwaitingScheduledPublish(BlogArticle article, LocalDateTime now) {
        if (article == null || article.getScheduledPublishTime() == null) {
            return false;
        }
        Integer reviewStatus = normalizeReviewStatus(article.getReviewStatus());
        return Integer.valueOf(STATUS_DRAFT).equals(normalizeStatus(article.getStatus()))
                && !article.getScheduledPublishTime().isAfter(now)
                && !reviewStatus.equals(ArticleReviewStatusEnum.REVIEWING.getValue())
                && !reviewStatus.equals(ArticleReviewStatusEnum.REJECTED.getValue());
    }

    private boolean isScheduledForFuture(LocalDateTime scheduledPublishTime, LocalDateTime now) {
        return scheduledPublishTime != null && scheduledPublishTime.isAfter(now);
    }
}
