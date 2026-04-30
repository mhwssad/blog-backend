package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.article.BlogArticleSeries;
import com.cybzacg.blogbackend.domain.article.BlogArticleSeriesItem;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.convert.ArticleSeriesModelMapper;
import com.cybzacg.blogbackend.module.article.model.common.ArticleSeriesArticleVO;
import com.cybzacg.blogbackend.module.article.model.common.ArticleSeriesSummaryVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleSeriesDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleSeriesVO;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesArticleRequest;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesSaveRequest;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesSortRequest;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleSeriesDetailVO;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleSeriesVO;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleSeriesItemRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleSeriesRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesItemService;
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesService;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 文章系列服务门面。<p>负责系列 CRUD、公开查询和访问控制，文章关联管理委托给 ArticleSeriesItemService。</p>
 */
@Service
@RequiredArgsConstructor
public class ArticleSeriesServiceImpl implements ArticleSeriesService {
    private static final int SERIES_STATUS_DISABLED = 0;
    private static final int SERIES_STATUS_NORMAL = 1;

    private final BlogArticleSeriesRepository blogArticleSeriesRepository;
    private final BlogArticleSeriesItemRepository blogArticleSeriesItemRepository;
    private final BlogArticleRepository blogArticleRepository;
    private final SysUserRepository sysUserRepository;
    private final AuthorPermissionService authorPermissionService;
    private final ArticleStatusMachine articleStatusMachine;
    private final ArticleAccessControlService articleAccessControlService;
    private final ArticleSeriesModelMapper articleSeriesModelMapper;
    private final ArticleSeriesItemService articleSeriesItemService;

    @Override
    public List<UserArticleSeriesVO> listMySeries() {
        Long userId = requireAuthorUserId();
        return blogArticleSeriesRepository.listByOwnerUserId(userId).stream()
                .map(articleSeriesModelMapper::toUserSeriesVO)
                .toList();
    }

    @Override
    public UserArticleSeriesDetailVO getMySeries(Long id) {
        Long userId = requireAuthorUserId();
        BlogArticleSeries series = getOwnedSeriesOrThrow(id, userId);
        return buildUserSeriesDetail(series);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserArticleSeriesDetailVO createSeries(ArticleSeriesSaveRequest request) {
        Long userId = requireAuthorUserId();
        validateSeriesSaveRequest(request);
        BlogArticleSeries series = articleSeriesModelMapper.toSeries(request);
        series.setOwnerUserId(userId);
        series.setStatus(normalizeSeriesStatus(series.getStatus()));
        series.setVisibilityScope(normalizeSeriesVisibilityScope(series.getVisibilityScope()));
        series.setArticleCount(0);
        series.setSortOrder(series.getSortOrder() == null ? 0 : series.getSortOrder());
        blogArticleSeriesRepository.save(series);
        return buildUserSeriesDetail(series);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserArticleSeriesDetailVO updateSeries(Long id, ArticleSeriesSaveRequest request) {
        Long userId = requireAuthorUserId();
        validateSeriesSaveRequest(request);
        BlogArticleSeries series = getOwnedSeriesOrThrow(id, userId);
        articleSeriesModelMapper.updateSeries(request, series);
        series.setStatus(normalizeSeriesStatus(series.getStatus()));
        series.setVisibilityScope(normalizeSeriesVisibilityScope(series.getVisibilityScope()));
        series.setSortOrder(series.getSortOrder() == null ? 0 : series.getSortOrder());
        blogArticleSeriesRepository.updateById(series);
        return buildUserSeriesDetail(series);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSeries(Long id) {
        Long userId = requireAuthorUserId();
        getOwnedSeriesOrThrow(id, userId);
        blogArticleSeriesItemRepository.removeBySeriesId(id);
        blogArticleSeriesRepository.removeById(id);
    }

    @Override
    public UserArticleSeriesDetailVO addArticle(Long id, ArticleSeriesArticleRequest request) {
        return articleSeriesItemService.addArticle(id, request);
    }

    @Override
    public UserArticleSeriesDetailVO removeArticle(Long id, Long articleId) {
        return articleSeriesItemService.removeArticle(id, articleId);
    }

    @Override
    public UserArticleSeriesDetailVO sortArticles(Long id, ArticleSeriesSortRequest request) {
        return articleSeriesItemService.sortArticles(id, request);
    }

    @Override
    public List<PublicArticleSeriesVO> listAuthorSeries(Long authorId) {
        requireExistingUser(authorId);
        Long currentUserId = SecurityUtils.getUserId();
        Map<Long, String> ownerNameMap = loadOwnerNames(List.of(authorId));
        return blogArticleSeriesRepository.listByOwnerUserId(authorId).stream()
                .filter(series -> canAccessSeries(series, currentUserId))
                .map(series -> {
                    PublicArticleSeriesVO vo = articleSeriesModelMapper.toPublicSeriesVO(series);
                    vo.setOwnerName(ownerNameMap.get(series.getOwnerUserId()));
                    return vo;
                })
                .toList();
    }

    @Override
    public PublicArticleSeriesDetailVO getPublicSeries(Long id) {
        BlogArticleSeries series = getSeriesOrThrow(id);
        Long currentUserId = SecurityUtils.getUserId();
        ExceptionThrowerCore.throwBusinessIfNot(
                canAccessSeries(series, currentUserId),
                ResultErrorCode.FORBIDDEN,
                "当前用户无权访问该系列"
        );
        return buildPublicSeriesDetail(series, currentUserId);
    }

    @Override
    public List<ArticleSeriesSummaryVO> listVisibleSeriesSummariesByArticleId(Long articleId, Long userId) {
        List<BlogArticleSeriesItem> items = blogArticleSeriesItemRepository.listByArticleId(articleId);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, BlogArticleSeries> seriesMap = loadSeriesMap(items.stream()
                .map(BlogArticleSeriesItem::getSeriesId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        return items.stream()
                .map(item -> seriesMap.get(item.getSeriesId()))
                .filter(Objects::nonNull)
                .filter(series -> canAccessSeries(series, userId))
                .sorted(Comparator.comparing(BlogArticleSeries::getSortOrder)
                        .thenComparing(BlogArticleSeries::getId))
                .map(articleSeriesModelMapper::toSeriesSummaryVO)
                .toList();
    }

    @Override
    public void cleanupArticleSeriesRelations(Long articleId) {
        articleSeriesItemService.cleanupArticleSeriesRelations(articleId);
    }

    private UserArticleSeriesDetailVO buildUserSeriesDetail(BlogArticleSeries series) {
        UserArticleSeriesDetailVO detailVO = articleSeriesModelMapper.toUserSeriesDetailVO(series);
        detailVO.setArticles(loadSeriesArticles(series.getId(), false, SecurityUtils.getUserId()));
        detailVO.setArticleCount(detailVO.getArticles().size());
        return detailVO;
    }

    private PublicArticleSeriesDetailVO buildPublicSeriesDetail(BlogArticleSeries series, Long currentUserId) {
        PublicArticleSeriesDetailVO detailVO = articleSeriesModelMapper.toPublicSeriesDetailVO(series);
        detailVO.setOwnerName(loadOwnerName(series.getOwnerUserId()));
        List<ArticleSeriesArticleVO> articles = loadSeriesArticles(series.getId(), true, currentUserId);
        detailVO.setArticles(articles);
        detailVO.setArticleCount(articles.size());
        return detailVO;
    }

    private List<ArticleSeriesArticleVO> loadSeriesArticles(Long seriesId, boolean onlyAccessible, Long currentUserId) {
        List<BlogArticleSeriesItem> items = blogArticleSeriesItemRepository.listBySeriesIdOrdered(seriesId);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, BlogArticle> articleMap = new HashMap<>();
        blogArticleRepository.listByIds(items.stream()
                .map(BlogArticleSeriesItem::getArticleId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
                .forEach(article -> articleMap.put(article.getId(), article));
        List<ArticleSeriesArticleVO> articles = new ArrayList<>();
        for (BlogArticleSeriesItem item : items) {
            BlogArticle article = articleMap.get(item.getArticleId());
            if (article == null) {
                continue;
            }
            if (onlyAccessible && !articleAccessControlService.canAccessArticle(article, currentUserId)) {
                continue;
            }
            ArticleSeriesArticleVO articleVO = articleSeriesModelMapper.toSeriesArticleVO(article);
            articleVO.setSeqNo(item.getSeqNo());
            articles.add(articleVO);
        }
        return articles;
    }

    private boolean canAccessSeries(BlogArticleSeries series, Long userId) {
        if (series == null) {
            return false;
        }
        boolean privileged = userId != null && userId.equals(series.getOwnerUserId());
        if (!privileged && SecurityUtils.hasAuthority("content:article:query")) {
            privileged = true;
        }
        if (privileged) {
            return true;
        }
        if (!Integer.valueOf(SERIES_STATUS_NORMAL).equals(series.getStatus())) {
            return false;
        }
        Integer visibilityScope = articleStatusMachine.normalizeVisibilityScope(series.getVisibilityScope());
        if (ArticleVisibilityScopeEnum.SELF_ONLY.getValue().equals(visibilityScope)
                || ArticleVisibilityScopeEnum.WHITELIST.getValue().equals(visibilityScope)) {
            return false;
        }
        return !ArticleVisibilityScopeEnum.LOGIN_REQUIRED.getValue().equals(visibilityScope) || userId != null;
    }

    private void validateSeriesSaveRequest(ArticleSeriesSaveRequest request) {
        ExceptionThrowerCore.throwBusinessIf(
                !StringUtils.hasText(request.getTitle()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "系列标题不能为空"
        );
        normalizeSeriesStatus(request.getStatus());
        normalizeSeriesVisibilityScope(request.getVisibilityScope());
    }

    private Integer normalizeSeriesStatus(Integer status) {
        Integer actualStatus = status == null ? SERIES_STATUS_NORMAL : status;
        ExceptionThrowerCore.throwBusinessIf(
                !Integer.valueOf(SERIES_STATUS_DISABLED).equals(actualStatus)
                        && !Integer.valueOf(SERIES_STATUS_NORMAL).equals(actualStatus),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "系列状态非法"
        );
        return actualStatus;
    }

    private Integer normalizeSeriesVisibilityScope(Integer visibilityScope) {
        Integer actualScope = articleStatusMachine.normalizeVisibilityScope(visibilityScope);
        ExceptionThrowerCore.throwBusinessIf(
                ArticleVisibilityScopeEnum.WHITELIST.getValue().equals(actualScope),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "系列当前暂不支持白名单可见"
        );
        return actualScope;
    }

    private BlogArticleSeries getOwnedSeriesOrThrow(Long id, Long ownerUserId) {
        BlogArticleSeries series = getSeriesOrThrow(id);
        ExceptionThrowerCore.throwBusinessIfNot(
                Objects.equals(series.getOwnerUserId(), ownerUserId),
                ResultErrorCode.FORBIDDEN,
                "只能操作自己的系列"
        );
        return series;
    }

    private BlogArticleSeries getSeriesOrThrow(Long id) {
        BlogArticleSeries series = blogArticleSeriesRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(series == null, ResultErrorCode.ILLEGAL_ARGUMENT, "系列不存在");
        return series;
    }

    private Long requireAuthorUserId() {
        Long userId = SecurityUtils.requireUserId();
        ExceptionThrowerCore.throwBusinessIfNot(
                authorPermissionService.hasAuthorRole(userId),
                ResultErrorCode.FORBIDDEN,
                "当前用户未开通作者权限"
        );
        return userId;
    }

    private void requireExistingUser(Long userId) {
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(
                user == null || Integer.valueOf(1).equals(user.getDeletedFlag()),
                ResultErrorCode.USER_NOT_FOUND,
                "作者不存在"
        );
    }

    private Map<Long, BlogArticleSeries> loadSeriesMap(Collection<Long> seriesIds) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BlogArticleSeries> seriesMap = new LinkedHashMap<>();
        blogArticleSeriesRepository.listByIds(seriesIds).forEach(series -> seriesMap.put(series.getId(), series));
        return seriesMap;
    }

    private Map<Long, String> loadOwnerNames(Collection<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> ownerNameMap = new HashMap<>();
        sysUserRepository.listByIds(ownerIds).forEach(user -> ownerNameMap.put(user.getId(), buildOwnerName(user)));
        return ownerNameMap;
    }

    private String loadOwnerName(Long ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        SysUser user = sysUserRepository.getById(ownerUserId);
        return buildOwnerName(user);
    }

    private String buildOwnerName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
