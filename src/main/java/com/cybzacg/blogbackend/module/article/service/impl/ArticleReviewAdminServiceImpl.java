package com.cybzacg.blogbackend.module.article.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleCategory;
import com.cybzacg.blogbackend.domain.BlogArticleReviewLog;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.article.ArticleReviewActionEnum;
import com.cybzacg.blogbackend.enums.article.ArticleReviewStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelMapper;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleReviewAdminDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleReviewAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleReviewDecisionRequest;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleReviewRepairRequest;
import com.cybzacg.blogbackend.module.article.model.common.ArticleReviewLogVO;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleCategoryRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleReviewLogRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleReviewAdminService;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.content.repository.SysTagRelationRepository;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文章审核后台服务实现。
 */
@Service
@RequiredArgsConstructor
public class ArticleReviewAdminServiceImpl implements ArticleReviewAdminService {
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final BlogArticleRepository blogArticleRepository;
    private final BlogArticleReviewLogRepository blogArticleReviewLogRepository;
    private final BlogArticleCategoryRepository blogArticleCategoryRepository;
    private final SysTagRelationRepository sysTagRelationRepository;
    private final SysUserRepository sysUserRepository;
    private final ArticleModelMapper articleModelMapper;
    private final ArticleAccessControlService articleAccessControlService;

    @Override
    public PageResult<ArticleAdminVO> pageReviews(ArticleReviewAdminPageQuery query) {
        ArticleReviewAdminPageQuery safeQuery = normalizeQuery(query);
        ArticleAdminPageQuery articleQuery = new ArticleAdminPageQuery();
        articleQuery.setCurrent(safeQuery.getCurrent());
        articleQuery.setSize(safeQuery.getSize());
        articleQuery.setKeyword(safeQuery.getKeyword());
        articleQuery.setAuthorId(safeQuery.getAuthorId());
        articleQuery.setReviewStatus(safeQuery.getReviewStatus());

        Page<BlogArticle> page = blogArticleRepository.pageAdminArticles(articleQuery, null);
        Map<Long, String> authorNameMap = loadAuthorNameMap(page.getRecords().stream()
                .map(BlogArticle::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        List<ArticleAdminVO> records = page.getRecords().stream()
                .map(articleModelMapper::toAdminVO)
                .peek(vo -> vo.setAuthorName(authorNameMap.get(vo.getAuthorId())))
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public ArticleReviewAdminDetailVO getReviewDetail(Long articleId) {
        BlogArticle article = requireArticle(articleId);
        ArticleReviewAdminDetailVO detailVO = new ArticleReviewAdminDetailVO();
        detailVO.setArticle(buildArticleDetail(article));
        detailVO.setReviewLogs(buildLogVOs(blogArticleReviewLogRepository.listByArticleId(articleId)));
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReview(Long articleId, ArticleReviewDecisionRequest request) {
        reviewArticle(articleId, ArticleReviewActionEnum.APPROVE, ArticleReviewStatusEnum.APPROVED, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReview(Long articleId, ArticleReviewDecisionRequest request) {
        ExceptionThrowerCore.throwBusinessIf(
                request == null || !StrUtils.hasText(request.getReviewComment()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "审核拒绝必须填写原因");
        reviewArticle(articleId, ArticleReviewActionEnum.REJECT, ArticleReviewStatusEnum.REJECTED, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void repairReviewStatus(Long articleId, ArticleReviewRepairRequest request) {
        ExceptionThrowerCore.throwBusinessIf(
                request == null,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "修正请求不能为空");
        ExceptionThrowerCore.throwBusinessIf(
                !StrUtils.hasText(request.getReviewComment()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "修正审核状态必须填写说明");
        ExceptionThrowerCore.throwBusinessIf(
                !ArticleReviewStatusEnum.contains(request.getTargetReviewStatus()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "目标审核状态非法");

        BlogArticle article = requireArticle(articleId);
        Integer currentReviewStatus = article.getReviewStatus() == null
                ? ArticleReviewStatusEnum.NOT_SUBMITTED.getValue()
                : article.getReviewStatus();
        Integer targetReviewStatus = request.getTargetReviewStatus();
        ExceptionThrowerCore.throwBusinessIf(
                Objects.equals(currentReviewStatus, targetReviewStatus),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "目标审核状态与当前状态一致，无需修正");

        LocalDateTime now = LocalDateTime.now();
        article.setReviewStatus(targetReviewStatus);
        blogArticleRepository.updateById(article);
        blogArticleReviewLogRepository.save(buildReviewLog(
                articleId,
                ArticleReviewActionEnum.REPAIR.getCode(),
                currentReviewStatus,
                targetReviewStatus,
                SecurityUtils.requireUserId(),
                request.getReviewComment(),
                now
        ));
    }

    private void reviewArticle(Long articleId,
                               ArticleReviewActionEnum action,
                               ArticleReviewStatusEnum targetStatus,
                               ArticleReviewDecisionRequest request) {
        BlogArticle article = requireArticle(articleId);
        Integer currentReviewStatus = article.getReviewStatus() == null
                ? ArticleReviewStatusEnum.NOT_SUBMITTED.getValue()
                : article.getReviewStatus();
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(currentReviewStatus, ArticleReviewStatusEnum.REVIEWING.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前文章不在待审核状态");

        LocalDateTime now = LocalDateTime.now();
        article.setReviewStatus(targetStatus.getValue());
        blogArticleRepository.updateById(article);
        blogArticleReviewLogRepository.save(buildReviewLog(
                articleId,
                action.getCode(),
                currentReviewStatus,
                targetStatus.getValue(),
                SecurityUtils.requireUserId(),
                request == null ? null : request.getReviewComment(),
                now
        ));
    }

    private ArticleReviewAdminPageQuery normalizeQuery(ArticleReviewAdminPageQuery query) {
        ArticleReviewAdminPageQuery safeQuery = query == null ? new ArticleReviewAdminPageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE));
        safeQuery.setReviewStatus(safeQuery.getReviewStatus() == null
                ? ArticleReviewStatusEnum.REVIEWING.getValue()
                : safeQuery.getReviewStatus());
        return safeQuery;
    }

    private BlogArticle requireArticle(Long articleId) {
        BlogArticle article = blogArticleRepository.getById(articleId);
        ExceptionThrowerCore.throwBusinessIf(article == null, ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        return article;
    }

    private ArticleDetailVO buildArticleDetail(BlogArticle article) {
        ArticleDetailVO detailVO = articleModelMapper.toDetailVO(article);
        detailVO.setAuthorName(loadAuthorName(article.getAuthorId()));
        detailVO.setCategoryIds(listCategoryIds(article.getId()));
        detailVO.setTagIds(sysTagRelationRepository.listTagIdsByTargetTypeAndTargetId("article", article.getId()));
        detailVO.setAccessList(articleAccessControlService.listArticleAccesses(article.getId()).stream()
                .map(articleModelMapper::toAccessItem)
                .toList());
        return detailVO;
    }

    private List<Long> listCategoryIds(Long articleId) {
        return blogArticleCategoryRepository.listByArticleIdOrdered(articleId).stream()
                .map(BlogArticleCategory::getCategoryId)
                .toList();
    }

    private String loadAuthorName(Long authorId) {
        if (authorId == null) {
            return null;
        }
        SysUser user = sysUserRepository.getById(authorId);
        return user == null ? null : (StrUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
    }

    private Map<Long, String> loadAuthorNameMap(Collection<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Map.of();
        }
        return sysUserRepository.listByIds(authorIds).stream()
                .collect(Collectors.toMap(
                        SysUser::getId,
                        user -> StrUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername(),
                        (left, right) -> left
                ));
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
