package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.article.BlogArticleReviewLog;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.article.ArticleReviewActionEnum;
import com.cybzacg.blogbackend.enums.article.ArticleReviewStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.model.common.ArticleReviewLogVO;
import com.cybzacg.blogbackend.module.article.model.user.ArticleReviewSubmitRequest;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleReviewLogRepository;
import com.cybzacg.blogbackend.module.article.service.UserArticleReviewService;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户文章审核服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserArticleReviewServiceImpl implements UserArticleReviewService {
    private final BlogArticleRepository blogArticleRepository;
    private final BlogArticleReviewLogRepository blogArticleReviewLogRepository;
    private final SysUserRepository sysUserRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(Long articleId, ArticleReviewSubmitRequest request) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticle article = requireOwnedArticle(articleId, userId);
        Integer currentReviewStatus = article.getReviewStatus() == null
                ? ArticleReviewStatusEnum.NOT_SUBMITTED.getValue()
                : article.getReviewStatus();
        ExceptionThrowerCore.throwBusinessIf(
                Objects.equals(currentReviewStatus, ArticleReviewStatusEnum.REVIEWING.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前文章已在审核中，请勿重复提交");
        ExceptionThrowerCore.throwBusinessIf(
                article.getScheduledPublishTime() != null && article.getScheduledPublishTime().isAfter(LocalDateTime.now()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "请先取消未来定时发布后再提交审核");

        String actionType = Objects.equals(currentReviewStatus, ArticleReviewStatusEnum.NOT_SUBMITTED.getValue())
                ? ArticleReviewActionEnum.SUBMIT.getCode()
                : ArticleReviewActionEnum.RESUBMIT.getCode();
        LocalDateTime now = LocalDateTime.now();
        article.setReviewStatus(ArticleReviewStatusEnum.REVIEWING.getValue());
        blogArticleRepository.updateById(article);

        blogArticleReviewLogRepository.save(buildReviewLog(
                articleId,
                actionType,
                currentReviewStatus,
                ArticleReviewStatusEnum.REVIEWING.getValue(),
                userId,
                request == null ? null : request.getReviewComment(),
                now
        ));
    }

    @Override
    public List<ArticleReviewLogVO> listReviewLogs(Long articleId) {
        Long userId = SecurityUtils.requireUserId();
        requireOwnedArticle(articleId, userId);
        return buildLogVOs(blogArticleReviewLogRepository.listByArticleId(articleId));
    }

    private BlogArticle requireOwnedArticle(Long articleId, Long userId) {
        BlogArticle article = blogArticleRepository.getById(articleId);
        ExceptionThrowerCore.throwBusinessIf(article == null, ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        ExceptionThrowerCore.throwBusinessIfNot(Objects.equals(article.getAuthorId(), userId),
                ResultErrorCode.FORBIDDEN, "只能操作自己的文章审核");
        return article;
    }

    private BlogArticleReviewLog buildReviewLog(Long articleId,
                                                String actionType,
                                                Integer fromReviewStatus,
                                                Integer toReviewStatus,
                                                Long operatorUserId,
                                                String reviewComment,
                                                LocalDateTime operatedAt) {
        BlogArticleReviewLog log = new BlogArticleReviewLog();
        log.setArticleId(articleId);
        log.setActionType(actionType);
        log.setFromReviewStatus(fromReviewStatus);
        log.setToReviewStatus(toReviewStatus);
        log.setOperatorUserId(operatorUserId);
        log.setReviewComment(StrUtils.trimToNull(reviewComment));
        log.setOperatedAt(operatedAt);
        return log;
    }

    private List<ArticleReviewLogVO> buildLogVOs(List<BlogArticleReviewLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        Set<Long> operatorUserIds = logs.stream()
                .map(BlogArticleReviewLog::getOperatorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SysUser> userMap = operatorUserIds.isEmpty()
                ? Map.of()
                : sysUserRepository.listByIds(operatorUserIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));

        return logs.stream()
                .map(log -> toReviewLogVO(log, userMap.get(log.getOperatorUserId())))
                .toList();
    }

    private ArticleReviewLogVO toReviewLogVO(BlogArticleReviewLog log, SysUser operator) {
        ArticleReviewLogVO vo = new ArticleReviewLogVO();
        vo.setId(log.getId());
        vo.setArticleId(log.getArticleId());
        vo.setActionType(log.getActionType());
        vo.setActionTypeLabel(ArticleReviewActionEnum.resolveLabel(log.getActionType()));
        vo.setFromReviewStatus(log.getFromReviewStatus());
        vo.setFromReviewStatusLabel(ArticleReviewStatusEnum.resolveLabel(log.getFromReviewStatus()));
        vo.setToReviewStatus(log.getToReviewStatus());
        vo.setToReviewStatusLabel(ArticleReviewStatusEnum.resolveLabel(log.getToReviewStatus()));
        vo.setOperatorUserId(log.getOperatorUserId());
        if (operator != null) {
            vo.setOperatorUsername(operator.getUsername());
            vo.setOperatorNickname(operator.getNickname());
        }
        vo.setReviewComment(log.getReviewComment());
        vo.setOperatedAt(log.getOperatedAt());
        return vo;
    }
}
