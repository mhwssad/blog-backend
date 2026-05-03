package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.domain.file.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.module.file.convert.FileModelConvert;
import com.cybzacg.blogbackend.module.file.model.admin.*;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileChunkRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.FileAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 后台文件管理服务实现。
 * 负责聚合文件本体、业务引用和上传任务，供管理端统一查询与清理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileAdminServiceImpl implements FileAdminService {
    private final FileInfoRepository fileInfoRepository;
    private final FileUploadTaskRepository fileUploadTaskRepository;
    private final FileChunkRepository fileChunkRepository;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final StorageManager storageManager;
    private final FileModelConvert fileModelConvert;

    /**
     * 按文件属性与业务引用维度分页查询后台文件列表。
     */
    @Override
    public PageResult<FileAdminVO> pageFiles(FileAdminPageQuery query) {
        var page = fileInfoRepository.pageAdminFiles(query); // 按管理维度分页查询
        List<FileAdminVO> records = page.getRecords().stream().map(fileModelConvert::toFileAdminVO).toList();
        return PageResult.of(page, records);
    }

    /**
     * 查询单个文件详情，并补齐引用记录与最近上传任务。
     */
    @Override
    public FileDetailVO getFile(Long id) {
        FileInfo file = fileInfoRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(file, FileResultCode.FILE_NOT_FOUND);
        FileDetailVO detail = fileModelConvert.toFileDetailVO(file);
        // 补齐业务引用记录，供管理端评估文件是否可以清理
        List<FileReferenceVO> references = fileBusinessInfoRepository.listByFileId(id)
                .stream()
                .map(fileModelConvert::toFileReferenceVO)
                .toList();
        detail.setReferences(references);
        // 补齐最近上传任务，帮助定位是否存在未完成的分片上传
        List<FileTaskAdminVO> tasks = fileUploadTaskRepository.listRecentByFileId(id, 20)
                .stream()
                .map(fileModelConvert::toFileTaskAdminVO)
                .toList();
        detail.setTasks(tasks);
        return detail;
    }

    /**
     * 按任务维度分页查询后台上传任务列表。
     */
    @Override
    public PageResult<FileTaskAdminVO> pageTasks(FileTaskPageQuery query) {
        var page = fileUploadTaskRepository.pageAdminTasks(query); // 按任务维度分页查询
        List<FileTaskAdminVO> records = page.getRecords().stream().map(fileModelConvert::toFileTaskAdminVO).toList();
        return PageResult.of(page, records);
    }

    /**
     * 更新文件状态，但不允许通过此接口将文件设为已删除或操作已删除文件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("更新文件状态, fileId={}, newStatus={}", id, status);
        ExceptionThrowerCore.throwBusinessIfNot(FileStatusEnum.contains(status), FileResultCode.FILE_STATUS_INVALID); // 状态值必须在枚举范围内
        ExceptionThrowerCore.throwBusinessIf(
                FileStatusEnum.DELETED.getValue().equals(status),
                FileResultCode.FILE_STATUS_INVALID,
                "文件删除请使用删除接口，状态更新接口不支持设置为已删除"
        );
        FileInfo file = fileInfoRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(file, FileResultCode.FILE_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(FileStatusEnum.DELETED.getValue().equals(file.getStatus()), FileResultCode.FILE_STATUS_INVALID); // 不允许操作已删除文件
        file.setStatus(status);
        fileInfoRepository.updateById(file);
        log.info("文件状态更新成功, fileId={}, status={}", id, status);
    }

    /**
     * 删除文件相关的业务引用、任务、分片和物理对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id) {
        log.info("开始删除文件, fileId={}", id);
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
                    log.warn("清理上传任务临时文件失败, taskId={}", task.getId());
                }
            }
            fileUploadTaskRepository.removeByIds(tasks.stream().map(FileUploadTask::getId).toList());
        }
        // 物理文件删除失败时不回滚元数据删除，避免后台任务被外部存储异常长期阻塞。
        boolean physicalDeleted = true;
        try {
            StorageService storageService = storageManager.getStorageService(file.getStorageKey());
            if (storageService != null) {
                storageService.delete(file.getFilePath());
            }
        } catch (Exception e) {
            physicalDeleted = false;
            log.warn("物理文件删除失败, fileId={}, path={}", id, file.getFilePath());
        }
        file.setReferenceCount(0);
        file.setStatus(physicalDeleted ? FileStatusEnum.DELETED.getValue() : FileStatusEnum.PHYSICAL_DELETE_PENDING.getValue());
        fileInfoRepository.updateById(file);
        if (physicalDeleted) {
            log.info("文件删除完成, fileId={}", id);
        } else {
            log.info("文件元数据标记为待物理删除, fileId={}, 将由定时任务重试", id);
        }
    }

}

