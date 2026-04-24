package com.cybzacg.blogbackend.module.file.service.impl;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminPageQuery;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileDetailVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileReferenceVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskAdminVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileChunkRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.FileAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
/**
 * 后台文件管理服务实现。
 * 负责聚合文件本体、业务引用和上传任务，供管理端统一查询与清理。
 */
@Service
@RequiredArgsConstructor
public class FileAdminServiceImpl implements FileAdminService {
    private final FileInfoRepository fileInfoRepository;
    private final FileUploadTaskRepository fileUploadTaskRepository;
    private final FileChunkRepository fileChunkRepository;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final StorageManager storageManager;
    /**
     * 按文件属性与业务引用维度分页查询后台文件列表。
     */
    @Override
    public PageResult<FileAdminVO> pageFiles(FileAdminPageQuery query) {
        validatePageFilesQuery(query);
        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 10L : query.getSize();
        query.setCurrent(current);
        query.setSize(size);
        var page = fileInfoRepository.pageAdminFiles(query);
        List<FileAdminVO> records = page.getRecords().stream().map(this::toAdminVO).toList();
        return PageResult.of(page, records);
    }
    /**
     * 查询单个文件详情，并补齐引用记录与最近上传任务。
     */
    @Override
    public FileDetailVO getFile(Long id) {
        FileInfo file = fileInfoRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(file, FileResultCode.FILE_NOT_FOUND);
        FileDetailVO detail = new FileDetailVO();
        // 详情主体与列表视图共用同一套基础字段映射，避免管理端展示口径不一致。
        copyToAdminVO(file, detail);
        List<FileReferenceVO> references = fileBusinessInfoRepository.listByFileId(id)
                .stream()
                .map(this::toReferenceVO)
                .toList();
        detail.setReferences(references);
        List<FileTaskAdminVO> tasks = fileUploadTaskRepository.listRecentByFileId(id, 20)
                .stream()
                .map(this::toTaskAdminVO)
                .toList();
        detail.setTasks(tasks);
        return detail;
    }
    /**
     * 按任务维度分页查询后台上传任务列表。
     */
    @Override
    public PageResult<FileTaskAdminVO> pageTasks(FileTaskPageQuery query) {
        validateTaskPageQuery(query);
        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 10L : query.getSize();
        query.setCurrent(current);
        query.setSize(size);
        var page = fileUploadTaskRepository.pageAdminTasks(query);
        List<FileTaskAdminVO> records = page.getRecords().stream().map(this::toTaskAdminVO).toList();
        return PageResult.of(page, records);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        ExceptionThrowerCore.throwBusinessIfNot(FileStatusEnum.contains(status), FileResultCode.FILE_STATUS_INVALID);
        ExceptionThrowerCore.throwBusinessIf(
                FileStatusEnum.DELETED.getValue().equals(status),
                FileResultCode.FILE_STATUS_INVALID,
                "文件删除请使用删除接口，状态更新接口不支持设置为已删除"
        );
        FileInfo file = fileInfoRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(file, FileResultCode.FILE_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(FileStatusEnum.DELETED.getValue().equals(file.getStatus()), FileResultCode.FILE_STATUS_INVALID);
        file.setStatus(status);
        fileInfoRepository.updateById(file);
    }
    /**
     * 删除文件相关的业务引用、任务、分片和物理对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id) {
        FileInfo file = fileInfoRepository.getById(id);
        if (file == null) {
            return;
        }
        // 先清理业务引用，避免后台删除后仍有业务记录指向失效文件。
        fileBusinessInfoRepository.deleteByFileId(id);
        // 再清理任务和临时分片，防止残留上传上下文继续占用空间。
        List<FileUploadTask> tasks = fileUploadTaskRepository.listByFileId(id);
        if (!tasks.isEmpty()) {
            for (FileUploadTask task : tasks) {
                fileChunkRepository.deleteByUploadTaskId(task.getId());
                try {
                    StorageService storageService = storageManager.getStorageService(task.getStorageKey());
                    if (storageService != null) {
                        storageService.deleteTempFiles(task.getUploadId());
                    }
                } catch (Exception ignored) {
                }
            }
            fileUploadTaskRepository.removeByIds(tasks.stream().map(FileUploadTask::getId).toList());
        }
        // 物理文件删除失败时不回滚元数据删除，避免后台任务被外部存储异常长期阻塞。
        try {
            StorageService storageService = storageManager.getStorageService(file.getStorageKey());
            if (storageService != null) {
                storageService.delete(file.getFilePath());
            }
        } catch (Exception ignored) {
        }
        file.setReferenceCount(0);
        file.setStatus(FileStatusEnum.DELETED.getValue());
        fileInfoRepository.updateById(file);
    }
    private FileAdminVO toAdminVO(FileInfo file) {
        FileAdminVO vo = new FileAdminVO();
        copyToAdminVO(file, vo);
        return vo;
    }
    /**
     * 复用文件列表和详情页共同需要的基础字段映射。
     */
    private void copyToAdminVO(FileInfo file, FileAdminVO vo) {
        vo.setId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setOriginalName(file.getOriginalName());
        vo.setFilePath(file.getFilePath());
        vo.setFileUrl(file.getFileUrl());
        vo.setStorageKey(file.getStorageKey());
        vo.setFileSize(file.getFileSize());
        vo.setFileType(file.getFileType());
        vo.setMimeType(file.getMimeType());
        vo.setFileExtension(file.getFileExtension());
        vo.setUploadUserId(file.getUploadUserId());
        vo.setIsPublic(file.getIsPublic());
        vo.setCategory(file.getCategory());
        vo.setStatus(file.getStatus());
        vo.setReferenceCount(file.getReferenceCount());
        vo.setCreatedAt(file.getCreatedAt());
    }
    /**
     * 将文件业务引用转换为后台详情页展示对象。
     */
    private FileReferenceVO toReferenceVO(FileBusinessInfo ref) {
        FileReferenceVO vo = new FileReferenceVO();
        vo.setId(ref.getId());
        vo.setUserId(ref.getUserId());
        vo.setReferenceType(ref.getReferenceType());
        vo.setReferenceId(ref.getReferenceId());
        vo.setIsPublic(ref.getIsPublic());
        vo.setCategory(ref.getCategory());
        vo.setRemark(ref.getRemark());
        vo.setCreatedAt(ref.getCreatedAt());
        return vo;
    }
    /**
     * 将上传任务转换为后台任务视图，便于排查失败或重试场景。
     */
    private FileTaskAdminVO toTaskAdminVO(FileUploadTask task) {
        FileTaskAdminVO vo = new FileTaskAdminVO();
        vo.setId(task.getId());
        vo.setUploadId(task.getUploadId());
        vo.setFileId(task.getFileId());
        vo.setUploadUserId(task.getUploadUserId());
        vo.setOriginalName(task.getOriginalName());
        vo.setFileSize(task.getFileSize());
        vo.setStorageKey(task.getStorageKey());
        vo.setIsQuickUpload(task.getIsQuickUpload());
        vo.setIsChunked(task.getIsChunked());
        vo.setUploadedChunks(task.getUploadedChunks());
        vo.setTotalChunks(task.getTotalChunks());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setCompleteTime(task.getCompleteTime());
        return vo;
    }
    private void validatePageFilesQuery(FileAdminPageQuery query) {
        ExceptionThrowerCore.throwBusinessIf(
                StringUtils.hasText(query.getCategory()) && !FileCategoryEnum.contains(query.getCategory()),
                ResultErrorCode.ILLEGAL_ARGUMENT
        );
        ExceptionThrowerCore.throwBusinessIf(
                StringUtils.hasText(query.getReferenceType()) && !FileReferenceTypeEnum.contains(query.getReferenceType()),
                ResultErrorCode.ILLEGAL_ARGUMENT
        );
        ExceptionThrowerCore.throwBusinessIf(
                query.getStatus() != null && !FileStatusEnum.contains(query.getStatus()),
                FileResultCode.FILE_STATUS_INVALID
        );
    }
    private void validateTaskPageQuery(FileTaskPageQuery query) {
        ExceptionThrowerCore.throwBusinessIf(
                query.getTaskStatus() != null && TaskStatusEnum.getByCode(query.getTaskStatus()) == null,
                FileResultCode.UPLOAD_TASK_STATUS_INVALID
        );
    }
}

