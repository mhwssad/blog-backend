package com.cybzacg.blogbackend.module.file.controller;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminPageQuery;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileDetailVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileStatusUpdateRequest;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskAdminVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.service.FileAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * 后台文件管理接口。
 * 主要用于文件查询、任务追踪和状态维护。
 */
@RestController
@RequestMapping("/api/sys/files")
@Tag(name = "后台文件管理")
@RequiredArgsConstructor
public class FileAdminController {
    private final FileAdminService fileAdminService;
    @GetMapping
    @Operation(summary = "分页查询文件")
    @PreAuthorize("@permission.hasPermission('content:file:query')")
    public Result<PageResult<FileAdminVO>> pageFiles(FileAdminPageQuery query) {
        return Result.success(fileAdminService.pageFiles(query));
    }
    @GetMapping("/{id}")
    @Operation(summary = "查询文件详情")
    @PreAuthorize("@permission.hasPermission('content:file:query')")
    public Result<FileDetailVO> getFile(@PathVariable Long id) {
        return Result.success(fileAdminService.getFile(id));
    }
    @GetMapping("/upload-tasks")
    @Operation(summary = "分页查询上传任务")
    @PreAuthorize("@permission.hasPermission('content:file:query')")
    public Result<PageResult<FileTaskAdminVO>> pageTasks(FileTaskPageQuery query) {
        return Result.success(fileAdminService.pageTasks(query));
    }
    @PutMapping("/{id}/status")
    @Operation(summary = "更新文件状态")
    @PreAuthorize("@permission.hasPermission('content:file:update')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody FileStatusUpdateRequest request) {
        fileAdminService.updateStatus(id, request.getStatus());
        return Result.success();
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件")
    @PreAuthorize("@permission.hasPermission('content:file:delete')")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileAdminService.deleteFile(id);
        return Result.success();
    }
}
