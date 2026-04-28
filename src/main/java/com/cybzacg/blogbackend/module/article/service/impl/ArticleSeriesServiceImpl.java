package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleSeries;
import com.cybzacg.blogbackend.domain.BlogArticleSeriesItem;
import com.cybzacg.blogbackend.domain.SysUser;
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
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesService;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.service.AuthorPermissionService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.IdCollectionUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章系列服务实现。
 */
@Service
@RequiredArgsConstructor
public class ArticleSeriesServiceImpl implements ArticleSeriesService {
    private static final int SERIES_STATUS_DISABLED = 0;
    private static final int SERIES_STATUS_NORMAL = 1;
    private static final int SORT_TEMP_OFFSET = 100000;

    private final BlogArticleSeriesRepository blogArticleSeriesRepository;
    private final BlogArticleSeriesItemRepository blogArticleSeriesItemRepository;
    private final BlogArticleRepository blogArticleRepository;
    private final SysUserRepository sysUserRepository;
    private final AuthorPermissionService authorPermissionService;
    private final ArticleStatusMachine articleStatusMachine;
    private final ArticleAccessControlService articleAccessControlService;
    private final ArticleSeriesModelMapper articleSeriesModelMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserArticleSeriesVO> listMySeries() {
        Long userId = requireAuthorUserId();
        return blogArticleSeriesRepository.listByOwnerUserId(userId).stream()
                .map(articleSeriesModelMapper::toUserSeriesVO)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserArticleSeriesDetailVO getMySeries(Long id) {
        Long userId = requireAuthorUserId();
        BlogArticleSeries series = getOwnedSeriesOrThrow(id, userId);
        return buildUserSeriesDetail(series);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSeries(Long id) {
        Long userId = requireAuthorUserId();
        getOwnedSeriesOrThrow(id, userId);
        blogArticleSeriesItemRepository.removeBySeriesId(id);
        blogArticleSeriesRepository.removeById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserArticleSeriesDetailVO addArticle(Long id, ArticleSeriesArticleRequest request) {
        Long userId = requireAuthorUserId();
        BlogArticleSeries series = getOwnedSeriesOrThrow(id, userId);
        BlogArticle article = requireOwnArticle(request.getArticleId(), userId);
        ExceptionThrowerCore.throwBusinessIf(
                blogArticleSeriesItemRepository.existsBySeriesIdAndArticleId(id, article.getId()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "该文章已在当前系列中"
        );

        BlogArticleSeriesItem item = new BlogArticleSeriesItem();
        item.setSeriesId(id);
        item.setArticleId(article.getId());
        item.setSeqNo(resolveNextSeqNo(id));
        blogArticleSeriesItemRepository.save(item);
        refreshArticleCount(id);
        return buildUserSeriesDetail(series);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserArticleSeriesDetailVO removeArticle(Long id, Long articleId) {
        Long userId = requireAuthorUserId();
        BlogArticleSeries series = getOwnedSeriesOrThrow(id, userId);
        blogArticleSeriesItemRepository.removeBySeriesIdAndArticleId(id, articleId);
        compactSeriesSeqNo(id);
        refreshArticleCount(id);
        return buildUserSeriesDetail(blogArticleSeriesRepository.getById(series.getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserArticleSeriesDetailVO sortArticles(Long id, ArticleSeriesSortRequest request) {
        Long userId = requireAuthorUserId();
        BlogArticleSeries series = getOwnedSeriesOrThrow(id, userId);
        List<Long> sortedArticleIds = IdCollectionUtils.requireUniqueNonNullIds(
                request.getArticleIds(),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文章ID不能为空",
                "排序列表中存在重复文章"
        );
        List<BlogArticleSeriesItem> items = blogArticleSeriesItemRepository.listBySeriesIdOrdered(id);
        Set<Long> currentArticleIds = items.stream()
                .map(BlogArticleSeriesItem::getArticleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        ExceptionThrowerCore.throwBusinessIf(
                currentArticleIds.size() != sortedArticleIds.size() || !currentArticleIds.containsAll(sortedArticleIds),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "排序列表必须覆盖该系列下的全部文章"
        );

        Map<Long, BlogArticleSeriesItem> itemMap = items.stream()
                .collect(Collectors.toMap(BlogArticleSeriesItem::getArticleId, item -> item));
        for (int i = 0; i < sortedArticleIds.size(); i++) {
            BlogArticleSeriesItem item = itemMap.get(sortedArticleIds.get(i));
            item.setSeqNo(SORT_TEMP_OFFSET + i + 1);
            blogArticleSeriesItemRepository.updateById(item);
        }
        for (int i = 0; i < sortedArticleIds.size(); i++) {
            BlogArticleSeriesItem item = itemMap.get(sortedArticleIds.get(i));
            item.setSeqNo(i + 1);
            blogArticleSeriesItemRepository.updateById(item);
        }
        return buildUserSeriesDetail(series);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ArticleSeriesSummaryVO> listVisibleSeriesSummariesByArticleId(Long articleId, Long userId) {
        List<BlogArticleSeriesItem> items = blogArticleSeriesItemRepository.listByArticleId(articleId);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, BlogArticleSeries> seriesMap = loadSeriesMap(items.stream()
                .map(BlogArticleSeriesItem::getSeriesId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        return items.stream()
                .map(item -> seriesMap.get(item.getSeriesId()))
                .filter(Objects::nonNull)
                .filter(series -> canAccessSeries(series, userId))
                .sorted(Comparator.comparing(BlogArticleSeries::getSortOrder)
                        .thenComparing(BlogArticleSeries::getId))
                .map(articleSeriesModelMapper::toSeriesSummaryVO)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupArticleSeriesRelations(Long articleId) {
        List<BlogArticleSeriesItem> items = blogArticleSeriesItemRepository.listByArticleId(articleId);
        if (items.isEmpty()) {
            return;
        }
        Set<Long> seriesIds = items.stream()
                .map(BlogArticleSeriesItem::getSeriesId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (BlogArticleSeriesItem item : items) {
            blogArticleSeriesItemRepository.removeById(item.getId());
        }
        for (Long seriesId : seriesIds) {
            compactSeriesSeqNo(seriesId);
            refreshArticleCount(seriesId);
        }
    }

    /**
     * 组装用户侧系列详情，返回全部自有文章。
     */
    private UserArticleSeriesDetailVO buildUserSeriesDetail(BlogArticleSeries series) {
        UserArticleSeriesDetailVO detailVO = articleSeriesModelMapper.toUserSeriesDetailVO(series);
        detailVO.setArticles(loadSeriesArticles(series.getId(), false, SecurityUtils.getUserId()));
        detailVO.setArticleCount(detailVO.getArticles().size());
        return detailVO;
    }

    /**
     * 组装公开系列详情，仅返回当前用户可访问的文章。
     */
    private PublicArticleSeriesDetailVO buildPublicSeriesDetail(BlogArticleSeries series, Long currentUserId) {
        PublicArticleSeriesDetailVO detailVO = articleSeriesModelMapper.toPublicSeriesDetailVO(series);
        detailVO.setOwnerName(loadOwnerName(series.getOwnerUserId()));
        List<ArticleSeriesArticleVO> articles = loadSeriesArticles(series.getId(), true, currentUserId);
        detailVO.setArticles(articles);
        detailVO.setArticleCount(articles.size());
        return detailVO;
    }

    /**
     * 读取系列内文章，并按系列顺序返回。
     */
    private List<ArticleSeriesArticleVO> loadSeriesArticles(Long seriesId, boolean onlyAccessible, Long currentUserId) {
        List<BlogArticleSeriesItem> items = blogArticleSeriesItemRepository.listBySeriesIdOrdered(seriesId);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, BlogArticle> articleMap = loadArticleMap(items.stream()
                .map(BlogArticleSeriesItem::getArticleId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
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

    /**
     * 判断当前用户是否可访问系列。
     */
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

    /**
     * 校验并标准化系列保存请求。
     */
    private void validateSeriesSaveRequest(ArticleSeriesSaveRequest request) {
        ExceptionThrowerCore.throwBusinessIf(
                !StringUtils.hasText(request.getTitle()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "系列标题不能为空"
        );
        normalizeSeriesStatus(request.getStatus());
        normalizeSeriesVisibilityScope(request.getVisibilityScope());
    }

    /**
     * 标准化系列状态，只允许启用和停用。
     */
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

    /**
     * 标准化系列可见范围，当前不开放白名单模式。
     */
    private Integer normalizeSeriesVisibilityScope(Integer visibilityScope) {
        Integer actualScope = articleStatusMachine.normalizeVisibilityScope(visibilityScope);
        ExceptionThrowerCore.throwBusinessIf(
                ArticleVisibilityScopeEnum.WHITELIST.getValue().equals(actualScope),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "系列当前暂不支持白名单可见"
        );
        return actualScope;
    }

    /**
     * 校验文章归属，确保系列只能挂当前作者自己的文章。
     */
    private BlogArticle requireOwnArticle(Long articleId, Long ownerUserId) {
        BlogArticle article = blogArticleRepository.getById(articleId);
        ExceptionThrowerCore.throwBusinessIf(article == null, ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        ExceptionThrowerCore.throwBusinessIfNot(
                Objects.equals(article.getAuthorId(), ownerUserId),
                ResultErrorCode.FORBIDDEN,
                "系列内只能加入自己的文章"
        );
        return article;
    }

    /**
     * 读取并校验系列归属。
     */
    private BlogArticleSeries getOwnedSeriesOrThrow(Long id, Long ownerUserId) {
        BlogArticleSeries series = getSeriesOrThrow(id);
        ExceptionThrowerCore.throwBusinessIfNot(
                Objects.equals(series.getOwnerUserId(), ownerUserId),
                ResultErrorCode.FORBIDDEN,
                "只能操作自己的系列"
        );
        return series;
    }

    /**
     * 读取系列实体，不存在时抛出异常。
     */
    private BlogArticleSeries getSeriesOrThrow(Long id) {
        BlogArticleSeries series = blogArticleSeriesRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(series == null, ResultErrorCode.ILLEGAL_ARGUMENT, "系列不存在");
        return series;
    }

    /**
     * 要求当前登录用户已经具备作者权限。
     */
    private Long requireAuthorUserId() {
        Long userId = SecurityUtils.requireUserId();
        ExceptionThrowerCore.throwBusinessIfNot(
                authorPermissionService.hasAuthorRole(userId),
                ResultErrorCode.FORBIDDEN,
                "当前用户未开通作者权限"
        );
        return userId;
    }

    /**
     * 读取作者并校验未删除。
     */
    private void requireExistingUser(Long userId) {
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(
                user == null || Integer.valueOf(1).equals(user.getDeletedFlag()),
                ResultErrorCode.USER_NOT_FOUND,
                "作者不存在"
        );
    }

    /**
     * 读取下一个系列顺序号。
     */
    private Integer resolveNextSeqNo(Long seriesId) {
        Integer maxSeqNo = blogArticleSeriesItemRepository.getMaxSeqNo(seriesId);
        return maxSeqNo == null ? 1 : maxSeqNo + 1;
    }

    /**
     * 删除文章后压实系列顺序，避免中间断号。
     */
    private void compactSeriesSeqNo(Long seriesId) {
        List<BlogArticleSeriesItem> items = blogArticleSeriesItemRepository.listBySeriesIdOrdered(seriesId);
        for (int i = 0; i < items.size(); i++) {
            BlogArticleSeriesItem item = items.get(i);
            int nextSeqNo = i + 1;
            if (!Objects.equals(item.getSeqNo(), nextSeqNo)) {
                item.setSeqNo(SORT_TEMP_OFFSET + nextSeqNo);
                blogArticleSeriesItemRepository.updateById(item);
            }
        }
        items = blogArticleSeriesItemRepository.listBySeriesIdOrdered(seriesId);
        for (int i = 0; i < items.size(); i++) {
            BlogArticleSeriesItem item = items.get(i);
            int nextSeqNo = i + 1;
            if (!Objects.equals(item.getSeqNo(), nextSeqNo)) {
                item.setSeqNo(nextSeqNo);
                blogArticleSeriesItemRepository.updateById(item);
            }
        }
    }

    /**
     * 重算并回写系列文章数。
     */
    private void refreshArticleCount(Long seriesId) {
        BlogArticleSeries series = blogArticleSeriesRepository.getById(seriesId);
        if (series == null) {
            return;
        }
        int articleCount = blogArticleSeriesItemRepository.listBySeriesIdOrdered(seriesId).size();
        series.setArticleCount(articleCount);
        blogArticleSeriesRepository.updateById(series);
    }

    /**
     * 批量读取系列映射。
     */
    private Map<Long, BlogArticleSeries> loadSeriesMap(Collection<Long> seriesIds) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BlogArticleSeries> seriesMap = new LinkedHashMap<>();
        blogArticleSeriesRepository.listByIds(seriesIds).forEach(series -> seriesMap.put(series.getId(), series));
        return seriesMap;
    }

    /**
     * 批量读取文章映射。
     */
    private Map<Long, BlogArticle> loadArticleMap(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BlogArticle> articleMap = new HashMap<>();
        blogArticleRepository.listByIds(articleIds).forEach(article -> articleMap.put(article.getId(), article));
        return articleMap;
    }

    /**
     * 批量读取创建人名称。
     */
    private Map<Long, String> loadOwnerNames(Collection<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> ownerNameMap = new HashMap<>();
        sysUserRepository.listByIds(ownerIds).forEach(user -> ownerNameMap.put(user.getId(), buildOwnerName(user)));
        return ownerNameMap;
    }

    /**
     * 读取单个创建人名称。
     */
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
