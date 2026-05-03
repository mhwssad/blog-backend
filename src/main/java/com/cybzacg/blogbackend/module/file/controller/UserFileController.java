package com.cybzacg.blogbackend.module.file.controller;

import com.cybzacg.blogbackend.core.validation.FileNotEmpty;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.file.model.user.*;
import com.cybzacg.blogbackend.module.file.service.UserFileService;
import com.cybzacg.blogbackend.utils.RequestContextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户侧文件控制器。
 *
 * <p>负责文件上传（普通/分片/秒传）、上传任务管理、文件列表查询与引用删除等用户侧接口。
 */
@Slf4j
@RestController
@Tag(name = "用户文件")
@RequiredArgsConstructor
@Validated
public class UserFileController {
    private final UserFileService userFileService;

    @PostMapping("/api/user/files/upload-tasks/init")
    @Operation(summary = "初始化上传任务")
    public Result<FileUploadInitVO> initUploadTask(@Valid @RequestBody FileUploadInitRequest request) {
        log.info("开始初始化上传任务, request={}", request);
        return Result.success(userFileService.initUploadTask(request, RequestContextUtils.getClientIp()));
    }

    @PostMapping("/api/user/files/upload-tasks/{uploadId}/quick-check")
    @Operation(summary = "秒传检测")
    public Result<FileUploadResultVO> quickCheck(@PathVariable String uploadId) {
        log.info("开始秒传检测, uploadId={}", uploadId);
        return Result.success(userFileService.quickCheck(uploadId, RequestContextUtils.getClientIp()));
    }

    @PostMapping("/api/user/files/upload-tasks/{uploadId}/file")
    @Operation(summary = "普通上传")
    public Result<FileUploadResultVO> uploadFile(@PathVariable String uploadId,
                                                 @RequestParam("file") @FileNotEmpty MultipartFile file) {
        log.info("开始普通上传文件, uploadId={}, fileName={}", uploadId, file.getOriginalFilename());
        return Result.success(userFileService.uploadFile(uploadId, file, RequestContextUtils.getClientIp()));
    }

    @PostMapping("/api/user/files/upload-tasks/{uploadId}/chunks/{chunkNumber}")
    @Operation(summary = "上传分片")
    public Result<ChunkUploadVO> uploadChunk(@PathVariable String uploadId,
                                             @PathVariable @Min(value = 1, message = "分片序号必须大于0") Integer chunkNumber,
                                             @RequestParam("file") @FileNotEmpty MultipartFile file,
                                             @RequestParam(value = "chunkMd5", required = false) String chunkMd5) {
        log.info("开始上传分片, uploadId={}, chunkNumber={}, chunkMd5={}", uploadId, chunkNumber, chunkMd5);
        return Result.success(userFileService.uploadChunk(uploadId, chunkNumber, file, chunkMd5, RequestContextUtils.getClientIp()));
    }

    @PostMapping("/api/user/files/upload-tasks/{uploadId}/complete")
    @Operation(summary = "完成上传")
    public Result<FileUploadResultVO> completeUpload(@PathVariable String uploadId) {
        log.info("开始完成上传, uploadId={}", uploadId);
        return Result.success(userFileService.completeUpload(uploadId, RequestContextUtils.getClientIp()));
    }

    @GetMapping("/api/user/files")
    @Operation(summary = "查询我的文件")
    public Result<PageResult<UserFileVO>> pageMyFiles(@Valid UserFilePageQuery query) {
        log.info("开始查询我的文件, query={}", query);
        return Result.success(userFileService.pageMyFiles(query));
    }

    @GetMapping("/api/user/files/upload-tasks")
    @Operation(summary = "查询我的上传任务")
    public Result<PageResult<UserFileTaskVO>> pageMyUploadTasks(@Valid UserFileTaskPageQuery query) {
        log.info("开始查询我的上传任务, query={}", query);
        return Result.success(userFileService.pageMyUploadTasks(query));
    }

    @DeleteMapping("/api/user/files/{businessId}")
    @Operation(summary = "删除我的文件引用")
    public Result<Void> deleteMyFile(@PathVariable Long businessId) {
        log.info("开始删除我的文件引用, businessId={}", businessId);
        userFileService.deleteMyFile(businessId);
        return Result.success();
    }
}
