package com.cybzacg.blogbackend.module.migration.controller;

import com.cybzacg.blogbackend.core.validation.FileNotEmpty;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.migration.model.admin.*;
import com.cybzacg.blogbackend.module.migration.service.BlogMigrationAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 外部博客迁移后台接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/migrations/blog")
@Tag(name = "后台博客迁移")
public class BlogMigrationAdminController {
    private final BlogMigrationAdminService blogMigrationAdminService;

    @PostMapping(value = "/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permission.hasPermission('content:migration:create')")
    public Result<BlogMigrationTaskVO> createTask(@Valid @ModelAttribute BlogMigrationCreateRequest request,
                                                  @RequestParam("file") @FileNotEmpty MultipartFile file) {
        return Result.success(blogMigrationAdminService.createTask(request, file, SecurityUtils.requireUserId()));
    }

    @PostMapping("/tasks/{id}/precheck")
    @PreAuthorize("@permission.hasPermission('content:migration:execute')")
    public Result<BlogMigrationPrecheckResultVO> precheck(@PathVariable Long id) {
        return Result.success(blogMigrationAdminService.precheck(id, SecurityUtils.requireUserId()));
    }

    @PostMapping("/tasks/{id}/execute")
    @PreAuthorize("@permission.hasPermission('content:migration:execute')")
    public Result<BlogMigrationTaskVO> execute(@PathVariable Long id) {
        return Result.success(blogMigrationAdminService.execute(id, SecurityUtils.requireUserId()));
    }

    @GetMapping("/tasks")
    @PreAuthorize("@permission.hasPermission('content:migration:query')")
    public Result<PageResult<BlogMigrationTaskVO>> pageTasks(@Valid BlogMigrationTaskPageQuery query) {
        return Result.success(blogMigrationAdminService.pageTasks(query));
    }

    @GetMapping("/tasks/{id}")
    @PreAuthorize("@permission.hasPermission('content:migration:query')")
    public Result<BlogMigrationTaskVO> getTask(@PathVariable Long id) {
        return Result.success(blogMigrationAdminService.getTask(id));
    }

    @GetMapping("/tasks/{id}/records")
    @PreAuthorize("@permission.hasPermission('content:migration:query')")
    public Result<PageResult<BlogMigrationRecordVO>> pageRecords(@PathVariable Long id,
                                                                 @Valid BlogMigrationRecordPageQuery query) {
        query.setTaskId(id);
        return Result.success(blogMigrationAdminService.pageRecords(query));
    }

    @GetMapping("/tasks/{id}/failures/export")
    @PreAuthorize("@permission.hasPermission('content:migration:export')")
    public ResponseEntity<byte[]> exportFailures(@PathVariable Long id) {
        byte[] bytes = blogMigrationAdminService.exportFailures(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("blog-migration-failures-" + id + ".xlsx")
                                .build()
                                .toString())
                .body(bytes);
    }
}
