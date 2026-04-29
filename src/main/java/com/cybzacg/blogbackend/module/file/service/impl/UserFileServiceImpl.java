package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.module.file.model.user.*;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.module.file.service.FileUploadService;
import com.cybzacg.blogbackend.module.file.service.UserFileQueryService;
import com.cybzacg.blogbackend.module.file.service.UserFileService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户文件服务门面。
 * 负责委托上传和查询操作到对应子服务，并保留用户侧文件引用删除能力。
 */
@Service
@RequiredArgsConstructor
public class UserFileServiceImpl implements UserFileService {
    private final UserFileQueryService userFileQueryService;
    private final FileUploadService fileUploadService;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final FileLifecycleService fileLifecycleService;

    @Override
    public FileUploadInitVO initUploadTask(FileUploadInitRequest request, String sourceIp) {
        Long userId = SecurityUtils.requireUserId();
        com.cybzacg.blogbackend.module.file.model.user.UserUploadInitRequest uploadRequest =
                new com.cybzacg.blogbackend.module.file.model.user.UserUploadInitRequest();
        uploadRequest.setOriginalName(request.getOriginalName());
        uploadRequest.setFileSize(request.getFileSize());
        uploadRequest.setFileMd5(request.getFileMd5());
        uploadRequest.setMimeType(request.getMimeType());
        uploadRequest.setReferenceType(request.getReferenceType());
        uploadRequest.setReferenceId(request.getReferenceId());
        uploadRequest.setCategory(request.getCategory());
        uploadRequest.setIsPublic(request.getIsPublic());
        uploadRequest.setTotalChunks(request.getTotalChunks());
        uploadRequest.setChunkSize(request.getChunkSize());
        uploadRequest.setRemark(request.getRemark());

        com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO taskVO =
                fileUploadService.initUploadTask(userId, uploadRequest);

        FileUploadInitVO vo = new FileUploadInitVO();
        vo.setTaskId(taskVO.getTaskId());
        vo.setUploadId(taskVO.getUploadId());
        vo.setChunkSize(taskVO.getChunkSize());
        vo.setTotalChunks(taskVO.getTotalChunks());
        vo.setUploadMode(taskVO.getUploadMode());
        vo.setQuickUploadAvailable(taskVO.getQuickUploadAvailable());
        vo.setCompleted(taskVO.getCompleted());
        vo.setTaskStatus(taskVO.getTaskStatus());
        vo.setFileId(taskVO.getFileId());
        vo.setFileUrl(taskVO.getFileUrl());
        vo.setBusinessId(taskVO.getBusinessId());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO quickCheck(String uploadId, String sourceIp) {
        Long userId = SecurityUtils.requireUserId();
        com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO taskVO =
                fileUploadService.quickCheck(userId, uploadId);

        FileUploadResultVO vo = new FileUploadResultVO();
        vo.setUploadId(taskVO.getUploadId());
        vo.setTaskId(taskVO.getTaskId());
        vo.setFileId(taskVO.getFileId());
        vo.setBusinessId(taskVO.getBusinessId());
        vo.setQuickUpload(taskVO.getQuickUpload());
        vo.setFileUrl(taskVO.getFileUrl());
        vo.setTaskStatus(taskVO.getTaskStatus());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO uploadFile(String uploadId, MultipartFile file, String sourceIp) {
        Long userId = SecurityUtils.requireUserId();
        String md5 = com.cybzacg.blogbackend.utils.FileUtils.normalizeMd5(null);

        com.cybzacg.blogbackend.domain.FileInfo fileInfo = new com.cybzacg.blogbackend.domain.FileInfo();
        // fileInfo would be populated from the MultipartFile

        com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO taskVO =
                fileUploadService.uploadFile(userId, md5, uploadId, fileInfo);

        FileUploadResultVO vo = new FileUploadResultVO();
        vo.setUploadId(taskVO.getUploadId());
        vo.setTaskId(taskVO.getTaskId());
        vo.setFileId(taskVO.getFileId());
        vo.setBusinessId(taskVO.getBusinessId());
        vo.setQuickUpload(taskVO.getQuickUpload());
        vo.setFileUrl(taskVO.getFileUrl());
        vo.setTaskStatus(taskVO.getTaskStatus());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkUploadVO uploadChunk(String uploadId, Integer chunkNumber, MultipartFile file, String chunkMd5, String sourceIp) {
        Long userId = SecurityUtils.requireUserId();

        com.cybzacg.blogbackend.domain.FileInfo chunkFileInfo = new com.cybzacg.blogbackend.domain.FileInfo();
        chunkFileInfo.setFileSize(file != null ? file.getSize() : null);
        chunkFileInfo.setMd5(chunkMd5);
        chunkFileInfo.setMimeType(file != null ? file.getContentType() : null);

        com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO taskVO =
                fileUploadService.uploadChunk(userId, uploadId, chunkNumber, chunkFileInfo);

        ChunkUploadVO vo = new ChunkUploadVO();
        vo.setUploadId(taskVO.getUploadId());
        vo.setChunkNumber(taskVO.getChunkNumber());
        vo.setUploadedChunks(taskVO.getUploadedChunks());
        vo.setTotalChunks(taskVO.getTotalChunks());
        vo.setTaskStatus(taskVO.getTaskStatus());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO completeUpload(String uploadId, String sourceIp) {
        Long userId = SecurityUtils.requireUserId();
        com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO taskVO =
                fileUploadService.completeUpload(userId, uploadId);

        FileUploadResultVO vo = new FileUploadResultVO();
        vo.setUploadId(taskVO.getUploadId());
        vo.setTaskId(taskVO.getTaskId());
        vo.setFileId(taskVO.getFileId());
        vo.setBusinessId(taskVO.getBusinessId());
        vo.setQuickUpload(taskVO.getQuickUpload());
        vo.setFileUrl(taskVO.getFileUrl());
        vo.setTaskStatus(taskVO.getTaskStatus());
        return vo;
    }

    @Override
    public PageResult<UserFileVO> pageMyFiles(UserFilePageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        return userFileQueryService.pageMyFiles(userId, query);
    }

    @Override
    public PageResult<UserFileTaskVO> pageMyUploadTasks(UserFileTaskPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        return userFileQueryService.pageMyUploadTasks(userId, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyFile(Long businessId) {
        Long userId = SecurityUtils.requireUserId();
        FileBusinessInfo ref = fileBusinessInfoRepository.getById(businessId);
        ExceptionThrowerCore.throwBusinessIf(ref == null || !userId.equals(ref.getUserId()),
                com.cybzacg.blogbackend.enums.file.FileResultCode.FILE_REFERENCE_NOT_FOUND);
        Long fileId = ref.getFileId();
        fileBusinessInfoRepository.removeById(businessId);
        fileLifecycleService.syncFileAfterReferenceRemoval(fileId);
    }
}