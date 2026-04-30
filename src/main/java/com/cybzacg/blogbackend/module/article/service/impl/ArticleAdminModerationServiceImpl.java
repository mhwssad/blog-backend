package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessManageService;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminModerationService;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文章后台审核与运营服务实现。
 */
@Service
@RequiredArgsConstructor
public class ArticleAdminModerationServiceImpl implements ArticleAdminModerationService {

    private final BlogArticleRepository blogArticleRepository;
    private final ArticleAccessManageService articleAccessManageService;
    private final SysAuditLogService sysAuditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleTop(Long id, boolean enabled, Long operatorId, String ip, String ua) {
        BlogArticle article = getArticleOrThrow(id);
        String beforeState = "{\"isTop\":" + article.getIsTop() + ",\"isRecommend\":" + article.getIsRecommend() + "}";
        article.setIsTop(enabled ? 1 : 0);
        blogArticleRepository.updateById(article);
        recordArticleAudit(operatorId, article.getAuthorId(), SysAuditOperationType.TOGGLE_ARTICLE_PIN.getCode(),
                id, beforeState, ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleRecommend(Long id, boolean enabled, Long operatorId, String ip, String ua) {
        BlogArticle article = getArticleOrThrow(id);
        String beforeState = "{\"isTop\":" + article.getIsTop() + ",\"isRecommend\":" + article.getIsRecommend() + "}";
        article.setIsRecommend(enabled ? 1 : 0);
        blogArticleRepository.updateById(article);
        recordArticleAudit(operatorId, article.getAuthorId(), SysAuditOperationType.TOGGLE_ARTICLE_RECOMMEND.getCode(),
                id, beforeState, ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignAccess(Long id, java.util.List<ArticleAccessItem> accessList) {
        BlogArticle article = getArticleOrThrow(id);
        ExceptionThrowerCore.throwBusinessIfNot(articleAccessManageService.supportsAccessList(article),
                ResultErrorCode.ILLEGAL_ARGUMENT, "当前文章不支持访问授权配置");
        articleAccessManageService.validateAccessItems(accessList);
        articleAccessManageService.rebuildArticleAccessBindings(id, accessList);
    }

    // ==================== private helpers ====================

    private BlogArticle getArticleOrThrow(Long id) {
        BlogArticle article = blogArticleRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(article, ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        return article;
    }

    private void recordArticleAudit(Long operatorId, Long targetUserId, String operationType,
                                    Long articleId, String beforeState, String ip, String ua) {
        SysAuditLogCreateRequest request = new SysAuditLogCreateRequest();
        request.setOperatorUserId(operatorId);
        request.setTargetUserId(targetUserId);
        request.setOperationType(operationType);
        request.setTargetTypeName("article");
        request.setTargetId(articleId);
        request.setBeforeState(beforeState);
        request.setMfaPassed(0);
        request.setRequestIp(ip);
        request.setUserAgent(ua);
        sysAuditLogService.record(request);
    }
}
