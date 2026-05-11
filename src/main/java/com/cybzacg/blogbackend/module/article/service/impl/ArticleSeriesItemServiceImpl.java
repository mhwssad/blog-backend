package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleSeries;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleSeriesItem;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleRepository;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleSeriesItemRepository;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleSeriesRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.convert.ArticleSeriesModelConvert;
import com.cybzacg.blogbackend.module.article.model.common.ArticleSeriesArticleVO;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesArticleRequest;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesSortRequest;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleSeriesDetailVO;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesItemService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.IdCollectionUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章系列-文章关联管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class ArticleSeriesItemServiceImpl implements ArticleSeriesItemService {
    private static final int SORT_TEMP_OFFSET = 100000;

    private final BlogArticleSeriesRepository blogArticleSeriesRepository;
    private final BlogArticleSeriesItemRepository blogArticleSeriesItemRepository;
    private final BlogArticleRepository blogArticleRepository;
    private final ArticleAccessControlService articleAccessControlService;
    private final ArticleSeriesModelConvert articleSeriesModelConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserArticleSeriesDetailVO addArticle(Long seriesId, ArticleSeriesArticleRequest request) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticleSeries series = getOwnedSeriesOrThrow(seriesId, userId);
        BlogArticle article = requireOwnArticle(request.getArticleId(), userId);
        ExceptionThrowerCore.throwBusinessIf(
                blogArticleSeriesItemRepository.existsBySeriesIdAndArticleId(seriesId, article.getId()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "该文章已在当前系列中"
        );

        BlogArticleSeriesItem item = new BlogArticleSeriesItem();
        item.setSeriesId(seriesId);
        item.setArticleId(article.getId());
        item.setSeqNo(resolveNextSeqNo(seriesId));
        blogArticleSeriesItemRepository.save(item);
        refreshArticleCount(seriesId);
        return buildUserSeriesDetail(series);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserArticleSeriesDetailVO removeArticle(Long seriesId, Long articleId) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticleSeries series = getOwnedSeriesOrThrow(seriesId, userId);
        blogArticleSeriesItemRepository.removeBySeriesIdAndArticleId(seriesId, articleId);
        compactSeriesSeqNo(seriesId);
        refreshArticleCount(seriesId);
        return buildUserSeriesDetail(blogArticleSeriesRepository.getById(series.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserArticleSeriesDetailVO sortArticles(Long seriesId, ArticleSeriesSortRequest request) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticleSeries series = getOwnedSeriesOrThrow(seriesId, userId);
        List<Long> sortedArticleIds = IdCollectionUtils.requireUniqueNonNullIds(
                request.getArticleIds(),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文章ID不能为空",
                "排序列表中存在重复文章"
        );
        List<BlogArticleSeriesItem> items = blogArticleSeriesItemRepository.listBySeriesIdOrdered(seriesId);
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

    private UserArticleSeriesDetailVO buildUserSeriesDetail(BlogArticleSeries series) {
        UserArticleSeriesDetailVO detailVO = articleSeriesModelConvert.toUserSeriesDetailVO(series);
        detailVO.setArticles(loadSeriesArticles(series.getId()));
        detailVO.setArticleCount(detailVO.getArticles().size());
        return detailVO;
    }

    private List<ArticleSeriesArticleVO> loadSeriesArticles(Long seriesId) {
        List<BlogArticleSeriesItem> items = blogArticleSeriesItemRepository.listBySeriesIdOrdered(seriesId);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, BlogArticle> articleMap = new HashMap<>();
        blogArticleRepository.listByIds(items.stream()
                .map(BlogArticleSeriesItem::getArticleId)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
                .forEach(article -> articleMap.put(article.getId(), article));
        List<ArticleSeriesArticleVO> articles = new ArrayList<>();
        for (BlogArticleSeriesItem item : items) {
            BlogArticle article = articleMap.get(item.getArticleId());
            if (article == null) {
                continue;
            }
            ArticleSeriesArticleVO articleVO = articleSeriesModelConvert.toSeriesArticleVO(article);
            articleVO.setSeqNo(item.getSeqNo());
            articles.add(articleVO);
        }
        return articles;
    }

    private BlogArticleSeries getOwnedSeriesOrThrow(Long id, Long ownerUserId) {
        BlogArticleSeries series = blogArticleSeriesRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(series == null, ResultErrorCode.ILLEGAL_ARGUMENT, "系列不存在");
        ExceptionThrowerCore.throwBusinessIfNot(
                Objects.equals(series.getOwnerUserId(), ownerUserId),
                ResultErrorCode.FORBIDDEN,
                "只能操作自己的系列"
        );
        return series;
    }

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

    private Integer resolveNextSeqNo(Long seriesId) {
        Integer maxSeqNo = blogArticleSeriesItemRepository.getMaxSeqNo(seriesId);
        return maxSeqNo == null ? 1 : maxSeqNo + 1;
    }

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

    private void refreshArticleCount(Long seriesId) {
        BlogArticleSeries series = blogArticleSeriesRepository.getById(seriesId);
        if (series == null) {
            return;
        }
        int articleCount = blogArticleSeriesItemRepository.listBySeriesIdOrdered(seriesId).size();
        series.setArticleCount(articleCount);
        blogArticleSeriesRepository.updateById(series);
    }
}
