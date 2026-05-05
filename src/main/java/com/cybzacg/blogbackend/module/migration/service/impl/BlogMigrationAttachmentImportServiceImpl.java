package com.cybzacg.blogbackend.module.migration.service.impl;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.domain.migration.BlogMigrationTask;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.migration.model.data.BlogMigrationAttachmentItem;
import com.cybzacg.blogbackend.module.migration.model.internal.BlogMigrationDownloadedAttachment;
import com.cybzacg.blogbackend.module.migration.service.BlogMigrationAttachmentImportService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 默认附件下载入库实现。
 *
 * <p>此服务只负责将外部 URL 下载成站内 {@code file_info}，不直接创建文章业务引用。
 * 迁移服务会先替换正文/封面 URL，再复用文章创建链路完成 {@code file_business_info} 同步。
 */
@Service
@RequiredArgsConstructor
public class BlogMigrationAttachmentImportServiceImpl implements BlogMigrationAttachmentImportService {
    private static final String FILE_CATEGORY_ATTACHMENT = "attachment";

    private final FileInfoRepository fileInfoRepository;
    private final StorageManager storageManager;

    @Override
    public BlogMigrationDownloadedAttachment downloadAndSave(BlogMigrationTask task, BlogMigrationAttachmentItem attachmentItem) {
        ExceptionThrowerCore.throwBusinessIfNull(task, ResultErrorCode.MIGRATION_TASK_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIfNull(attachmentItem, ResultErrorCode.MIGRATION_ATTACHMENT_DOWNLOAD_FAILED, "附件不能为空");
        ExceptionThrowerCore.throwBusinessIfBlank(attachmentItem.getUrl(), ResultErrorCode.MIGRATION_ATTACHMENT_DOWNLOAD_FAILED, "附件URL不能为空");

        try {
            byte[] bytes = downloadBytes(attachmentItem.getUrl());
            String md5 = FileUtils.toHex(MessageDigest.getInstance("MD5").digest(bytes));
            FileInfo existing = fileInfoRepository.findByMd5AndStatus(md5, FileStatusEnum.NORMAL.getValue());
            if (existing != null) {
                return new BlogMigrationDownloadedAttachment(existing.getId(), existing.getFileUrl());
            }

            String fileName = resolveFileName(attachmentItem);
            String objectName = buildObjectName(task.getId(), fileName);
            StorageService storageService = requireStorageService();
            String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            String fileUrl = storageService.upload(new ByteArrayInputStream(bytes), objectName, mimeType);

            FileInfo fileInfo = new FileInfo();
            fileInfo.setUploadTaskId(null);
            fileInfo.setFileName(fileName);
            fileInfo.setOriginalName(fileName);
            fileInfo.setFilePath(objectName);
            fileInfo.setStorageKey(storageManager.getCurrentStorageKey());
            fileInfo.setFileUrl(fileUrl);
            fileInfo.setFileSize((long) bytes.length);
            fileInfo.setFileType("other");
            fileInfo.setMimeType(mimeType);
            fileInfo.setFileExtension(FileUtils.normalizeExt(FileUtils.getExtension(fileName)));
            fileInfo.setMd5(md5);
            fileInfo.setReferenceCount(0);
            fileInfo.setIsPublic(1);
            fileInfo.setCategory(FILE_CATEGORY_ATTACHMENT);
            fileInfo.setDownloadCount(0);
            fileInfo.setUploadUserId(task.getAuthorId());
            fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
            fileInfoRepository.save(fileInfo);
            return new BlogMigrationDownloadedAttachment(fileInfo.getId(), fileUrl);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultErrorCode.MIGRATION_ATTACHMENT_DOWNLOAD_FAILED.getCode(), "附件下载入库失败", ex);
        }
    }

    /**
     * 从外部 URL 下载附件内容。v1 为同步迁移，后续如需限速或异步队列可替换本方法边界。
     */
    private byte[] downloadBytes(String url) throws Exception {
        try (InputStream inputStream = URI.create(url).toURL().openStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        }
    }

    private StorageService requireStorageService() {
        StorageService storageService = storageManager.getStorageService();
        ExceptionThrowerCore.throwBusinessIf(storageService == null,
                ResultErrorCode.MIGRATION_ATTACHMENT_DOWNLOAD_FAILED, "当前存储节点不可用");
        ExceptionThrowerCore.throwBusinessIfBlank(storageManager.getCurrentStorageKey(),
                ResultErrorCode.MIGRATION_ATTACHMENT_DOWNLOAD_FAILED, "当前存储节点未配置");
        return storageService;
    }

    private String resolveFileName(BlogMigrationAttachmentItem attachmentItem) {
        if (StringUtils.hasText(attachmentItem.getOriginalName())) {
            return FileUtils.getFileName(attachmentItem.getOriginalName().trim());
        }
        String path = URI.create(attachmentItem.getUrl()).getPath();
        String guessed = FileUtils.getFileName(path);
        return StringUtils.hasText(guessed) ? guessed : "attachment.bin";
    }

    private String buildObjectName(Long taskId, String fileName) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String extension = FileUtils.normalizeExt(FileUtils.getExtension(fileName));
        String suffix = StringUtils.hasText(extension) ? "." + extension : "";
        return "migration/" + taskId + "/" + date + "/" + UUID.randomUUID() + suffix;
    }
}
