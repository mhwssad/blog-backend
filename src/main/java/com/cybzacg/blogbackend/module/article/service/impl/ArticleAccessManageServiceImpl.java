package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.article.BlogArticleAccess;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelConvert;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleAccessRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessManageService;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.utils.CollectionUtils;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 文章访问名单管理服务实现。
 *
 * <p>集中处理访问名单的能力判定、数据校验和“先删后建”式重建逻辑。
 */
@Service
@RequiredArgsConstructor
public class ArticleAccessManageServiceImpl implements ArticleAccessManageService {
    private final BlogArticleAccessRepository blogArticleAccessRepository;
    private final SysUserRepository sysUserRepository;
    private final ArticleModelConvert articleModelConvert;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsAccessList(BlogArticle article) {
        if (article == null) {
            return false;
        }
        return Integer.valueOf(4).equals(CollectionUtils.defaultIfNull(article.getAccessLevel(), 0))
                || ArticleVisibilityScopeEnum.WHITELIST.getValue().equals(article.getVisibilityScope());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateAccessItems(List<ArticleAccessItem> accessList) {
        if (accessList == null || accessList.isEmpty()) {
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();
        for (ArticleAccessItem item : accessList) {
            ExceptionThrowerCore.throwBusinessIfNull(item.getUserId(), ResultErrorCode.ILLEGAL_ARGUMENT, "授权用户不能为空");
            Integer accessType = CollectionUtils.defaultIfNull(item.getAccessType(), 1);
            ExceptionThrowerCore.throwBusinessIf(!Integer.valueOf(1).equals(accessType) && !Integer.valueOf(2).equals(accessType),
                    ResultErrorCode.ILLEGAL_ARGUMENT, "访问类型非法");
            item.setAccessType(accessType);
            String key = item.getUserId() + ":" + accessType;
            ExceptionThrowerCore.throwBusinessIf(!keys.add(key), ResultErrorCode.ILLEGAL_ARGUMENT, "存在重复的访问授权记录");
            userIds.add(item.getUserId());
        }
        List<SysUser> users = sysUserRepository.listByIds(userIds);
        long availableUsers = users.stream()
                .filter(user -> !Integer.valueOf(1).equals(user.getDeletedFlag()))
                .count();
        ExceptionThrowerCore.throwBusinessIf(availableUsers != userIds.size(), ResultErrorCode.USER_NOT_FOUND, "授权用户不存在");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rebuildArticleAccessBindings(Long articleId, List<ArticleAccessItem> accessList) {
        blogArticleAccessRepository.removeByArticleId(articleId);
        if (accessList == null || accessList.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<BlogArticleAccess> records = accessList.stream()
                .map(item -> articleModelConvert.toArticleAccess(articleId, item, now))
                .toList();
        blogArticleAccessRepository.saveBatch(records);
    }
}
