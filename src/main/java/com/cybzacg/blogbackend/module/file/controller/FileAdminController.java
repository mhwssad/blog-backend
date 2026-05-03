package com.cybzacg.blogbackend.module.file.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.file.model.admin.*;
import com.cybzacg.blogbackend.module.file.service.FileAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 文件后台管理控制器。
 *
 * <p>负责文件查询、上传任务追踪、状态维护与删除等后台运营接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/files")
@Tag(name = "后台文件管理")
@RequiredArgsConstructor
@Validated
public class FileAdminController {
    private final FileAdminService fileAdminService;

    @GetMapping
    @Operation(summary = "分页查询文件")
    @PreAuthorize("@permission.hasPermission('content:file:query')")
    public Result<PageResult<FileAdminVO>> pageFiles(@Valid FileAdminPageQuery query) {
        log.info("开始分页查询文件, query={}", query);
        return Result.success(fileAdminService.pageFiles(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询文件详情")
    @PreAuthorize("@permission.hasPermission('content:file:query')")
    public Result<FileDetailVO> getFile(@PathVariable Long id) {
        log.info("开始查询文件详情, id={}", id);
        return Result.success(fileAdminService.getFile(id));
    }

    @GetMapping("/upload-tasks")
    @Operation(summary = "分页查询上传任务")
    @PreAuthorize("@permission.hasPermission('content:file:query')")
    public Result<PageResult<FileTaskAdminVO>> pageTasks(@Valid FileTaskPageQuery query) {
        log.info("开始分页查询上传任务, query={}", query);
        return Result.success(fileAdminService.pageTasks(query));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新文件状态")
    @PreAuthorize("@permission.hasPermission('content:file:update')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody FileStatusUpdateRequest request) {
        log.info("开始更新文件状态, id={}, status={}", id, request.getStatus());
        fileAdminService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件")
    @PreAuthorize("@permission.hasPermission('content:file:delete')")
    public Result<Void> deleteFile(@PathVariable Long id) {
        log.info("开始删除文件, id={}", id);
        fileAdminService.deleteFile(id);
        return Result.success();
    }
}
