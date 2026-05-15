package com.cybzacg.blogbackend.module.migration;

import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.content.SysCategory;
import com.cybzacg.blogbackend.dto.domain.content.SysTag;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationRecord;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationTask;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.migration.BlogMigrationRecordStatusEnum;
import com.cybzacg.blogbackend.enums.migration.BlogMigrationTaskStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.dto.repository.content.SysCategoryRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysTagRepository;
import com.cybzacg.blogbackend.module.migration.convert.BlogMigrationModelConvert;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationCreateRequest;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationPrecheckResultVO;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationRecordVO;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationTaskVO;
import com.cybzacg.blogbackend.module.migration.model.data.BlogMigrationAttachmentItem;
import com.cybzacg.blogbackend.module.migration.model.internal.BlogMigrationDownloadedAttachment;
import com.cybzacg.blogbackend.dto.repository.migration.BlogMigrationAttachmentRepository;
import com.cybzacg.blogbackend.dto.repository.migration.BlogMigrationRecordRepository;
import com.cybzacg.blogbackend.dto.repository.migration.BlogMigrationTaskRepository;
import com.cybzacg.blogbackend.module.migration.service.BlogMigrationAttachmentImportService;
import com.cybzacg.blogbackend.module.migration.service.impl.BlogMigrationAdminServiceImpl;
import com.cybzacg.blogbackend.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.validation.Validator;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogMigrationAdminServiceImplTest {
    @Mock
    private BlogMigrationTaskRepository taskRepository;
    @Mock
    private BlogMigrationRecordRepository recordRepository;
    @Mock
    private BlogMigrationAttachmentRepository attachmentRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private AuthorPermissionService authorPermissionService;
    @Mock
    private SysCategoryRepository sysCategoryRepository;
    @Mock
    private SysTagRepository sysTagRepository;
    @Mock
    private ArticleAdminService articleAdminService;
    @Mock
    private BlogMigrationAttachmentImportService attachmentImportService;
    @Mock
    private BlogMigrationModelConvert convert;
    @Mock
    private Validator validator;

    private BlogMigrationAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BlogMigrationAdminServiceImpl(
                taskRepository,
                recordRepository,
                attachmentRepository,
                sysUserRepository,
                authorPermissionService,
                sysCategoryRepository,
                sysTagRepository,
                articleAdminService,
                attachmentImportService,
                convert,
                validator
        );
    }

    @Test
    void createTaskShouldParseJsonAndSaveTask() {
        mockAuthor(10L);
        when(convert.toTaskVO(any())).thenReturn(new BlogMigrationTaskVO());
        BlogMigrationCreateRequest request = new BlogMigrationCreateRequest();
        request.setAuthorId(10L);

        service.createTask(request, file(validJson()), 99L);

        ArgumentCaptor<BlogMigrationTask> captor = ArgumentCaptor.forClass(BlogMigrationTask.class);
        verify(taskRepository).save(captor.capture());
        BlogMigrationTask task = captor.getValue();
        assertEquals("wordpress", task.getSourcePlatform());
        assertEquals(10L, task.getAuthorId());
        assertEquals(1, task.getTotalCount());
        assertEquals(BlogMigrationTaskStatusEnum.CREATED.getValue(), task.getStatus());
    }

    @Test
    void createTaskShouldRejectMissingAuthor() {
        BlogMigrationCreateRequest request = new BlogMigrationCreateRequest();
        request.setAuthorId(10L);
        when(sysUserRepository.getById(10L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createTask(request, file(validJson()), 99L));

        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTaskShouldRejectInvalidJson() {
        mockAuthor(10L);
        BlogMigrationCreateRequest request = new BlogMigrationCreateRequest();
        request.setAuthorId(10L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createTask(request, file("{bad"), 99L));

        assertEquals(ResultErrorCode.MIGRATION_FILE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void precheckShouldReturnErrorsForMissingTaxonomyDuplicateAndInvalidUrl() {
        mockAuthor(10L);
        BlogMigrationTask task = task(1L, BlogMigrationTaskStatusEnum.CREATED.getValue(), invalidPrecheckJson());
        when(taskRepository.getById(1L)).thenReturn(task);
        when(sysCategoryRepository.listByTypeStatusAndCodes(eq("article"), eq(1), anyCollection())).thenReturn(List.of());
        when(sysTagRepository.listByNames(any())).thenReturn(List.of());
        when(recordRepository.findByTaskIdAndIdempotentKey(any(), any())).thenReturn(null);
        when(recordRepository.save(any())).thenAnswer(inv -> {
            BlogMigrationRecord record = inv.getArgument(0);
            record.setId(System.nanoTime());
            return true;
        });
        when(convert.toRecordVO(any())).thenAnswer(inv -> {
            BlogMigrationRecord record = inv.getArgument(0);
            BlogMigrationRecordVO vo = new BlogMigrationRecordVO();
            vo.setErrorMessage(record.getErrorMessage());
            return vo;
        });

        BlogMigrationPrecheckResultVO result = service.precheck(1L, 99L);

        assertFalse(result.isPassed());
        assertEquals(5, result.getErrors().size());
        verify(taskRepository).updateById(task);
        assertEquals(BlogMigrationTaskStatusEnum.CREATED.getValue(), task.getStatus());
    }

    @Test
    void executeShouldImportArticleAndReplaceAttachmentUrls() {
        BlogMigrationTask task = task(1L, BlogMigrationTaskStatusEnum.PRECHECKED.getValue(), validJson());
        when(taskRepository.getById(1L)).thenReturn(task);
        when(recordRepository.findByIdempotentKey("wordpress:post-1")).thenReturn(null);
        when(recordRepository.findByTaskIdAndIdempotentKey(any(), any())).thenReturn(null);
        when(recordRepository.save(any())).thenAnswer(inv -> {
            BlogMigrationRecord record = inv.getArgument(0);
            record.setId(100L);
            return true;
        });
        when(sysCategoryRepository.listByTypeStatusAndCodes(eq("article"), eq(1), anyCollection())).thenReturn(List.of(category(7L, "tech")));
        when(sysTagRepository.listByNames(any())).thenReturn(List.of(tag(8L, "Java")));
        when(attachmentImportService.downloadAndSave(eq(task), any(BlogMigrationAttachmentItem.class)))
                .thenReturn(new BlogMigrationDownloadedAttachment(9L, "https://local/a.jpg"));
        ArticleDetailVO article = new ArticleDetailVO();
        article.setId(20L);
        when(articleAdminService.createArticle(any())).thenReturn(article);
        when(convert.toTaskVO(any())).thenReturn(new BlogMigrationTaskVO());

        service.execute(1L, 99L);

        ArgumentCaptor<ArticleSaveRequest> requestCaptor = ArgumentCaptor.forClass(ArticleSaveRequest.class);
        verify(articleAdminService).createArticle(requestCaptor.capture());
        assertTrue(requestCaptor.getValue().getContent().contains("https://local/a.jpg"));
        assertEquals("https://local/a.jpg", requestCaptor.getValue().getCoverImage());
        assertEquals(BlogMigrationTaskStatusEnum.COMPLETED.getValue(), task.getStatus());
        assertEquals(1, task.getSuccessCount());
        assertEquals(0, task.getFailCount());
    }

    @Test
    void executeShouldSkipGlobalImportedIdempotentKey() {
        BlogMigrationTask task = task(1L, BlogMigrationTaskStatusEnum.PRECHECKED.getValue(), validJson());
        BlogMigrationRecord existing = new BlogMigrationRecord();
        existing.setTaskId(2L);
        existing.setStatus(BlogMigrationRecordStatusEnum.SUCCESS.getValue());
        existing.setTargetArticleId(30L);
        when(taskRepository.getById(1L)).thenReturn(task);
        when(recordRepository.findByIdempotentKey("wordpress:post-1")).thenReturn(existing);
        when(recordRepository.findByTaskIdAndIdempotentKey(any(), any())).thenReturn(null);
        when(convert.toTaskVO(any())).thenReturn(new BlogMigrationTaskVO());

        service.execute(1L, 99L);

        verify(articleAdminService, never()).createArticle(any());
        assertEquals(1, task.getSkipCount());
        assertEquals(BlogMigrationTaskStatusEnum.COMPLETED.getValue(), task.getStatus());
    }

    @Test
    void executeShouldRecordFailureWhenAttachmentDownloadFails() {
        BlogMigrationTask task = task(1L, BlogMigrationTaskStatusEnum.PRECHECKED.getValue(), validJson());
        when(taskRepository.getById(1L)).thenReturn(task);
        when(recordRepository.findByIdempotentKey("wordpress:post-1")).thenReturn(null);
        when(recordRepository.findByTaskIdAndIdempotentKey(any(), any())).thenReturn(null);
        when(recordRepository.save(any())).thenAnswer(inv -> {
            BlogMigrationRecord record = inv.getArgument(0);
            record.setId(100L);
            return true;
        });
        when(attachmentImportService.downloadAndSave(eq(task), any(BlogMigrationAttachmentItem.class)))
                .thenThrow(new BusinessException(ResultErrorCode.MIGRATION_ATTACHMENT_DOWNLOAD_FAILED));
        when(convert.toTaskVO(any())).thenReturn(new BlogMigrationTaskVO());

        service.execute(1L, 99L);

        verify(articleAdminService, never()).createArticle(any());
        assertEquals(1, task.getFailCount());
        assertEquals(BlogMigrationTaskStatusEnum.FAILED.getValue(), task.getStatus());
    }

    @Test
    void executeShouldRejectInvalidStatus() {
        BlogMigrationTask task = task(1L, BlogMigrationTaskStatusEnum.CREATED.getValue(), validJson());
        when(taskRepository.getById(1L)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.execute(1L, 99L));

        assertEquals(ResultErrorCode.MIGRATION_TASK_STATUS_INVALID.getCode(), ex.getCode());
    }

    private void mockAuthor(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setDeletedFlag(0);
        when(sysUserRepository.getById(userId)).thenReturn(user);
        when(authorPermissionService.hasAuthorRole(userId)).thenReturn(true);
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "migration.json", "application/json",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private BlogMigrationTask task(Long id, Integer status, String json) {
        BlogMigrationTask task = new BlogMigrationTask();
        task.setId(id);
        task.setAuthorId(10L);
        task.setStatus(status);
        task.setFileContentJson(json);
        return task;
    }

    private SysCategory category(Long id, String code) {
        SysCategory category = new SysCategory();
        category.setId(id);
        category.setCode(code);
        return category;
    }

    private SysTag tag(Long id, String name) {
        SysTag tag = new SysTag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }

    private String validJson() {
        return """
                {
                  "sourcePlatform": "wordpress",
                  "posts": [
                    {
                      "externalPostId": "post-1",
                      "title": "标题",
                      "summary": "摘要",
                      "content": "正文 ![a](https://example.com/a.jpg)",
                      "coverImageUrl": "https://example.com/a.jpg",
                      "categoryCodes": ["tech"],
                      "tagNames": ["Java"],
                      "isOriginal": 1,
                      "status": 0,
                      "attachments": [{"url":"https://example.com/a.jpg","originalName":"a.jpg"}]
                    }
                  ]
                }
                """;
    }

    private String invalidPrecheckJson() {
        return JsonUtils.toJson(java.util.Map.of(
                "sourcePlatform", "wordpress",
                "posts", List.of(
                        java.util.Map.of("externalPostId", "post-1", "title", "A", "categoryCodes", List.of("missing")),
                        java.util.Map.of("externalPostId", "post-2", "title", "B", "tagNames", List.of("Missing")),
                        java.util.Map.of("externalPostId", "post-3", "title", "C"),
                        java.util.Map.of("externalPostId", "post-3", "title", "D"),
                        java.util.Map.of("externalPostId", "post-4", "title", "E", "coverImageUrl", "ftp://bad")
                )
        ));
    }
}
