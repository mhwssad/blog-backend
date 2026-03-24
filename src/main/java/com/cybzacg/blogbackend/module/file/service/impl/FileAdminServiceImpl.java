package com.cybzacg.blogbackend.module.file.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileChunk;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminPageQuery;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileDetailVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileReferenceVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskAdminVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.service.FileAdminService;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import com.cybzacg.blogbackend.module.file.service.FileChunkService;
import com.cybzacg.blogbackend.module.file.service.FileInfoService;
import com.cybzacg.blogbackend.module.file.service.FileUploadTaskService;
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
    private final FileInfoService fileInfoService;
    private final FileUploadTaskService fileUploadTaskService;
    private final FileChunkService fileChunkService;
    private final FileBusinessInfoService fileBusinessInfoService;
    private final StorageManager storageManager;
    /**
     * 按文件属性与业务引用维度分页查询后台文件列表。
     */
    @Override
    public PageResult<FileAdminVO> pageFiles(FileAdminPageQuery query) {
        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 10L : query.getSize();
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .eq(query.getUploadUserId() != null, FileInfo::getUploadUserId, query.getUploadUserId())
                .eq(query.getStatus() != null, FileInfo::getStatus, query.getStatus())
                .eq(query.getIsPublic() != null, FileInfo::getIsPublic, query.getIsPublic())
                .eq(StringUtils.hasText(query.getCategory()), FileInfo::getCategory, FileCategoryEnum.normalize(query.getCategory()))
                .and(StringUtils.hasText(query.getKeyword()), w -> w.like(FileInfo::getOriginalName, query.getKeyword())
                        .or()
                        .like(FileInfo::getFileName, query.getKeyword()))
                .orderByDesc(FileInfo::getUpdatedAt)
                .orderByDesc(FileInfo::getId);
        if (StringUtils.hasText(query.getReferenceType())) {
            String type = FileReferenceTypeEnum.normalize(query.getReferenceType());
            wrapper.inSql(FileInfo::getId, "select distinct file_id from file_business_info where reference_type='" + type + "'");
        }
        Page<FileInfo> page = fileInfoService.page(new Page<>(current, size), wrapper);
        List<FileAdminVO> records = page.getRecords().stream().map(this::toAdminVO).toList();
        return PageResult.of(page, records);
    }
    /**
     * 查询单个文件详情，并补齐引用记录与最近上传任务。
     */
    @Override
    public FileDetailVO getFile(Long id) {
        FileInfo file = fileInfoService.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(file, FileResultCode.FILE_NOT_FOUND);
        FileDetailVO detail = new FileDetailVO();
        // 详情主体与列表视图共用同一套基础字段映射，避免管理端展示口径不一致。
        copyToAdminVO(file, detail);
        List<FileReferenceVO> references = fileBusinessInfoService.lambdaQuery()
                .eq(FileBusinessInfo::getFileId, id)
                .orderByDesc(FileBusinessInfo::getCreatedAt)
                .orderByDesc(FileBusinessInfo::getId)
                .list()
                .stream()
                .map(this::toReferenceVO)
                .toList();
        detail.setReferences(references);
        List<FileTaskAdminVO> tasks = fileUploadTaskService.lambdaQuery()
                .eq(FileUploadTask::getFileId, id)
                .orderByDesc(FileUploadTask::getCreatedAt)
                .orderByDesc(FileUploadTask::getId)
                .last("limit 20")
                .list()
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
        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 10L : query.getSize();
        LambdaQueryWrapper<FileUploadTask> wrapper = new LambdaQueryWrapper<FileUploadTask>()
                .eq(query.getUploadUserId() != null, FileUploadTask::getUploadUserId, query.getUploadUserId())
                .eq(query.getTaskStatus() != null, FileUploadTask::getTaskStatus, query.getTaskStatus())
                .eq(query.getIsQuickUpload() != null, FileUploadTask::getIsQuickUpload, query.getIsQuickUpload())
                .eq(query.getIsChunked() != null, FileUploadTask::getIsChunked, query.getIsChunked())
                .orderByDesc(FileUploadTask::getCreatedAt)
                .orderByDesc(FileUploadTask::getId);
        Page<FileUploadTask> page = fileUploadTaskService.page(new Page<>(current, size), wrapper);
        List<FileTaskAdminVO> records = page.getRecords().stream().map(this::toTaskAdminVO).toList();
        return PageResult.of(page, records);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        ExceptionThrowerCore.throwBusinessIfNot(FileStatusEnum.contains(status), FileResultCode.FILE_STATUS_INVALID);
        FileInfo file = fileInfoService.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(file, FileResultCode.FILE_NOT_FOUND);
        file.setStatus(status);
        fileInfoService.updateById(file);
    }
    /**
     * 删除文件相关的业务引用、任务、分片和物理对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id) {
        FileInfo file = fileInfoService.getById(id);
        if (file == null) {
            return;
        }
        // 先清理业务引用，避免后台删除后仍有业务记录指向失效文件。
        fileBusinessInfoService.remove(new LambdaQueryWrapper<FileBusinessInfo>().eq(FileBusinessInfo::getFileId, id));
        // 再清理任务和临时分片，防止残留上传上下文继续占用空间。
        List<FileUploadTask> tasks = fileUploadTaskService.lambdaQuery().eq(FileUploadTask::getFileId, id).list();
        if (!tasks.isEmpty()) {
            for (FileUploadTask task : tasks) {
                fileChunkService.remove(new LambdaQueryWrapper<FileChunk>().eq(FileChunk::getUploadTaskId, task.getId()));
                try {
                    StorageService storageService = storageManager.getStorageService(task.getStorageKey());
                    if (storageService != null) {
                        storageService.deleteTempFiles(task.getUploadId());
                    }
                } catch (Exception ignored) {
                }
            }
            fileUploadTaskService.removeByIds(tasks.stream().map(FileUploadTask::getId).toList());
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
        fileInfoService.updateById(file);
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
}
