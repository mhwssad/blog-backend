package com.cybzacg.blogbackend.module.article;

import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.ai.ContentChangeAction;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import com.cybzacg.blogbackend.module.ai.event.ContentChangeEvent;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelConvert;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleAccessRepository;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleCategoryRepository;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessManageService;
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesService;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.module.article.service.impl.ArticleAdminCrudServiceImpl;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.dto.repository.content.SysCollectionFolderRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysCollectionRepository;
import com.cybzacg.blogbackend.dto.repository.comment.SysCommentRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysUserFootprintRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysInteractionRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysCategoryRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysTagRelationRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysTagRepository;
import com.cybzacg.blogbackend.dto.repository.file.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.dto.repository.file.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleAdminCrudServiceImplKnowledgeEventTest {
    @Mock
    private BlogArticleRepository blogArticleRepository;
    @Mock
    private BlogArticleCategoryRepository blogArticleCategoryRepository;
    @Mock
    private BlogArticleAccessRepository blogArticleAccessRepository;
    @Mock
    private SysTagRelationRepository sysTagRelationRepository;
    @Mock
    private SysCategoryRepository sysCategoryRepository;
    @Mock
    private SysTagRepository sysTagRepository;
    @Mock
    private SysCommentRepository sysCommentRepository;
    @Mock
    private SysCollectionFolderRepository sysCollectionFolderRepository;
    @Mock
    private SysCollectionRepository sysCollectionRepository;
    @Mock
    private SysInteractionRepository sysInteractionRepository;
    @Mock
    private SysUserFootprintRepository sysUserFootprintRepository;
    @Mock
    private FileBusinessInfoRepository fileBusinessInfoRepository;
    @Mock
    private FileInfoRepository fileInfoRepository;
    @Mock
    private FileLifecycleService fileLifecycleService;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private AuthorPermissionService authorPermissionService;
    @Mock
    private ArticleModelConvert articleModelConvert;
    @Mock
    private ArticleAccessControlService articleAccessControlService;
    @Mock
    private ArticleAccessManageService articleAccessManageService;
    @Mock
    private ArticleSeriesService articleSeriesService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ArticleAdminCrudServiceImpl articleAdminCrudService;

    @BeforeEach
    void setUp() {
        articleAdminCrudService = new ArticleAdminCrudServiceImpl(
                blogArticleRepository,
                blogArticleCategoryRepository,
                blogArticleAccessRepository,
                sysTagRelationRepository,
                sysCategoryRepository,
                sysTagRepository,
                sysCommentRepository,
                sysCollectionFolderRepository,
                sysCollectionRepository,
                sysInteractionRepository,
                sysUserFootprintRepository,
                fileBusinessInfoRepository,
                fileInfoRepository,
                fileLifecycleService,
                sysUserRepository,
                sysConfigService,
                authorPermissionService,
                articleModelConvert,
                articleAccessControlService,
                articleAccessManageService,
                articleSeriesService,
                new ArticleStatusMachine(),
                eventPublisher
        );
    }

    @Test
    void createArticleShouldPublishKnowledgeEventOnlyForPublicPublishedArticle() {
        ArticleSaveRequest request = request(ArticleVisibilityScopeEnum.PUBLIC.getValue());
        BlogArticle article = article(100L, ArticleVisibilityScopeEnum.PUBLIC.getValue());
        when(sysUserRepository.getById(7L)).thenReturn(author());
        when(authorPermissionService.hasAuthorRole(7L)).thenReturn(false);
        when(sysConfigService.getValueOrDefault(any(), any())).thenReturn("0");
        when(articleModelConvert.toArticle(request)).thenReturn(article);
        when(blogArticleRepository.getById(100L)).thenReturn(article);
        when(articleModelConvert.toDetailVO(article)).thenReturn(new ArticleDetailVO());
        when(articleAccessControlService.listArticleAccesses(100L)).thenReturn(List.of());
        when(articleSeriesService.listVisibleSeriesSummariesByArticleId(100L, 7L)).thenReturn(List.of());

        articleAdminCrudService.createArticle(request);

        assertTrue(hasKnowledgeEvent(ContentChangeAction.PUBLISH, 100L));
    }

    @Test
    void createArticleShouldNotPublishKnowledgeEventForWhitelistArticle() {
        ArticleSaveRequest request = request(ArticleVisibilityScopeEnum.WHITELIST.getValue());
        BlogArticle article = article(101L, ArticleVisibilityScopeEnum.WHITELIST.getValue());
        when(sysUserRepository.getById(7L)).thenReturn(author());
        when(authorPermissionService.hasAuthorRole(7L)).thenReturn(false);
        when(sysConfigService.getValueOrDefault(any(), any())).thenReturn("0");
        when(articleModelConvert.toArticle(request)).thenReturn(article);
        when(blogArticleRepository.getById(101L)).thenReturn(article);
        when(articleAccessManageService.supportsAccessList(article)).thenReturn(true);
        when(articleModelConvert.toDetailVO(article)).thenReturn(new ArticleDetailVO());
        when(articleAccessControlService.listArticleAccesses(101L)).thenReturn(List.of());
        when(articleSeriesService.listVisibleSeriesSummariesByArticleId(101L, 7L)).thenReturn(List.of());

        articleAdminCrudService.createArticle(request);

        assertTrue(publishedEvents().stream().noneMatch(ContentChangeEvent.class::isInstance));
    }

    @Test
    void updateStatusShouldNotPublishKnowledgeEventWhenLoginOnlyArticleBecomesPublished() {
        BlogArticle article = article(102L, ArticleVisibilityScopeEnum.LOGIN_REQUIRED.getValue());
        article.setStatus(0);
        when(blogArticleRepository.getById(102L)).thenReturn(article);

        articleAdminCrudService.updateStatus(102L, 1);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private ArticleSaveRequest request(Integer visibilityScope) {
        ArticleSaveRequest request = new ArticleSaveRequest();
        request.setTitle("标题");
        request.setAuthorId(7L);
        request.setStatus(1);
        request.setVisibilityScope(visibilityScope);
        request.setAccessLevel(0);
        if (ArticleVisibilityScopeEnum.WHITELIST.getValue().equals(visibilityScope)) {
            request.setAccessList(List.of(new com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem()));
        }
        return request;
    }

    private BlogArticle article(Long id, Integer visibilityScope) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setAuthorId(7L);
        article.setStatus(1);
        article.setReviewStatus(0);
        article.setVisibilityScope(visibilityScope);
        article.setAccessLevel(0);
        return article;
    }

    private SysUser author() {
        SysUser user = new SysUser();
        user.setId(7L);
        user.setDeletedFlag(0);
        return user;
    }

    private boolean hasKnowledgeEvent(ContentChangeAction action, Long sourceId) {
        return publishedEvents().stream().anyMatch(event ->
                event instanceof ContentChangeEvent changeEvent
                        && action.equals(changeEvent.getAction())
                        && sourceId.equals(changeEvent.getSourceId()));
    }

    private List<Object> publishedEvents() {
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());
        return eventCaptor.getAllValues();
    }
}
