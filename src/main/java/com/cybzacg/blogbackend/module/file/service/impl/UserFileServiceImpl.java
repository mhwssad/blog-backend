package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.*;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.module.file.service.FileUploadService;
import com.cybzacg.blogbackend.module.file.service.UserFileQueryService;
import com.cybzacg.blogbackend.module.file.service.UserFileService;
import com.cybzacg.blogbackend.utils.BeanConverterUtils;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 用户文件服务门面。
 * 负责委托上传和查询操作到对应子服务，并保留用户侧文件引用删除能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFileServiceImpl implements UserFileService {
    private final UserFileQueryService userFileQueryService;
    private final FileUploadService fileUploadService;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final FileLifecycleService fileLifecycleService;

    @Override
    public FileUploadInitVO initUploadTask(FileUploadInitRequest request, String sourceIp) {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.requireUserId();
        log.info("[文件上传] 用户 {} 初始化上传任务，原始文件名: {}，文件大小: {}，来源IP: {}",
                userId, request.getOriginalName(), request.getFileSize(), sourceIp);

        // 将前端请求转换为内部上传初始化请求（属性拷贝）
        UserUploadInitRequest uploadRequest = new UserUploadInitRequest();
        BeanConverterUtils.copyProperties(request, uploadRequest);

        // 委托 FileUploadService 执行任务初始化
        UserTaskVO taskVO = fileUploadService.initUploadTask(userId, uploadRequest);
        log.info("[文件上传] 用户 {} 初始化任务完成，uploadId: {}，taskId: {}，上传模式: {}",
                userId, taskVO.getUploadId(), taskVO.getTaskId(), taskVO.getUploadMode());

        // 将任务结果转换为初始化响应 VO
        return BeanConverterUtils.convert(taskVO, FileUploadInitVO::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO quickCheck(String uploadId, String sourceIp) {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.requireUserId();
        log.info("[文件秒传校验] 用户 {} 请求校验 uploadId: {}，来源IP: {}", userId, uploadId, sourceIp);

        // 委托 FileUploadService 执行秒传校验
        com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO taskVO =
                fileUploadService.quickCheck(userId, uploadId);
        log.info("[文件秒传校验] 用户 {} 校验完成，fileId: {}，秒传: {}", userId, taskVO.getFileId(), taskVO.getQuickUpload());

        // 将任务结果转换为上传结果 VO
        return BeanConverterUtils.convert(taskVO, FileUploadResultVO::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO uploadFile(String uploadId, MultipartFile file, String sourceIp) {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.requireUserId();
        log.info("[文件上传] 用户 {} 上传文件，原始文件名: {}，大小: {}，uploadId: {}，来源IP: {}",
                userId, file.getOriginalFilename(), file.getSize(), uploadId, sourceIp);

        // 构建文件信息对象（从 MultipartFile 提取元数据）
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileSize(file.getSize());
        fileInfo.setMimeType(file.getContentType());
        fileInfo.setOriginalName(file.getOriginalFilename());

        UserTaskVO taskVO;
        try {
            // 委托 FileUploadService 执行文件上传（包含物理存储和数据库记录）
            taskVO = fileUploadService.uploadFile(userId, null, uploadId, fileInfo, file.getInputStream());
        } catch (IOException e) {
            // IO 异常（流读取失败）记日志并抛出业务异常
            log.error("[文件上传] 用户 {} 上传失败，uploadId: {}，错误: {}", userId, uploadId, e.getMessage());
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.FILE_UPLOAD_FAILED, e.getMessage());
            return null; // unreachable
        }

        log.info("[文件上传] 用户 {} 上传完成，fileId: {}，任务状态: {}", userId, taskVO.getFileId(), taskVO.getTaskStatus());
        // 将任务结果转换为上传结果 VO
        return BeanConverterUtils.convert(taskVO, FileUploadResultVO::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkUploadVO uploadChunk(String uploadId, Integer chunkNumber, MultipartFile file, String chunkMd5, String sourceIp) {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.requireUserId();
        log.info("[分片上传] 用户 {} 上传分片，uploadId: {}，分片号: {}，分片MD5: {}，大小: {}，来源IP: {}",
                userId, uploadId, chunkNumber, chunkMd5, file.getSize(), sourceIp);

        // 构建分片文件信息对象（包含分片大小、MD5、MIME类型）
        FileInfo chunkFileInfo = new FileInfo();
        chunkFileInfo.setFileSize(file.getSize());
        chunkFileInfo.setMd5(chunkMd5);
        chunkFileInfo.setMimeType(file.getContentType());

        UserTaskVO taskVO;
        try {
            // 委托 FileUploadService 执行分片上传（包含分片存储和元数据记录）
            taskVO = fileUploadService.uploadChunk(userId, uploadId, chunkNumber, chunkFileInfo, file.getInputStream());
        } catch (IOException e) {
            // IO 异常记日志并抛出业务异常
            log.error("[分片上传] 用户 {} 上传失败，uploadId: {}，分片号: {}，错误: {}", userId, uploadId, chunkNumber, e.getMessage());
            ExceptionThrowerCore.throwBusinessEx(
                    com.cybzacg.blogbackend.enums.file.FileResultCode.CHUNK_UPLOAD_FAILED, e.getMessage());
            return null;
        }

        log.info("[分片上传] 用户 {} 分片上传完成，uploadId: {}，已上传: {}/{}，任务状态: {}",
                userId, uploadId, taskVO.getUploadedChunks(), taskVO.getTotalChunks(), taskVO.getTaskStatus());
        // 将任务结果转换为分片上传结果 VO
        return BeanConverterUtils.convert(taskVO, ChunkUploadVO::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO completeUpload(String uploadId, String sourceIp) {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.requireUserId();
        log.info("[文件上传完成] 用户 {} 请求完成上传，uploadId: {}，来源IP: {}", userId, uploadId, sourceIp);

        // 委托 FileUploadService 执行分片合并和上传完成流程
        com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO taskVO =
                fileUploadService.completeUpload(userId, uploadId);
        log.info("[文件上传完成] 用户 {} 上传完成，fileId: {}，任务状态: {}", userId, taskVO.getFileId(), taskVO.getTaskStatus());

        // 将任务结果转换为上传结果 VO
        return BeanConverterUtils.convert(taskVO, FileUploadResultVO::new);
    }

    @Override
    public PageResult<UserFileVO> pageMyFiles(UserFilePageQuery query) {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.requireUserId();
        // 委托 UserFileQueryService 执行分页查询
        return userFileQueryService.pageMyFiles(userId, query);
    }

    @Override
    public PageResult<UserFileTaskVO> pageMyUploadTasks(UserFileTaskPageQuery query) {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.requireUserId();
        // 委托 UserFileQueryService 执行分页查询
        return userFileQueryService.pageMyUploadTasks(userId, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyFile(Long businessId) {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.requireUserId();
        log.info("[文件删除] 用户 {} 请求删除文件引用，businessId: {}", userId, businessId);

        // 查询文件业务引用，校验存在性和归属权
        FileBusinessInfo ref = fileBusinessInfoRepository.getById(businessId);
        ExceptionThrowerCore.throwBusinessIf(ref == null || !userId.equals(ref.getUserId()),
                FileResultCode.FILE_REFERENCE_NOT_FOUND);

        // 提取关联的文件ID，删除业务引用记录
        Long fileId = ref.getFileId();
        fileBusinessInfoRepository.removeById(businessId);

        // 触发文件生命周期同步（引用计数刷新 + 归零后物理回收判断）
        fileLifecycleService.syncFileAfterReferenceRemoval(fileId);

        log.info("[文件删除] 用户 {} 删除文件引用完成，businessId: {}，fileId: {}", userId, businessId, fileId);
    }
}
