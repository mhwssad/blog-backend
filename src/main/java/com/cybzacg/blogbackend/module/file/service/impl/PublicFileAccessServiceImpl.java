package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.dto.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleRepository;
import com.cybzacg.blogbackend.dto.repository.file.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.dto.repository.file.FileInfoRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.file.model.data.FileContentVO;
import com.cybzacg.blogbackend.module.file.service.PublicFileAccessService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * 公开文件访问服务实现。
 * 处理文件存在性校验、关联文章访问控制和文件内容获取。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicFileAccessServiceImpl implements PublicFileAccessService {

    private final FileInfoRepository fileInfoRepository;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final BlogArticleRepository blogArticleRepository;
    private final ArticleAccessControlService articleAccessControlService;
    private final StorageManager storageManager;

    @Override
    public FileContentVO getFileContent(Long fileId) {
        log.info("获取文件内容, fileId={}", fileId);

        FileInfo fileInfo = fileInfoRepository.getById(fileId);
        ExceptionThrowerCore.throwBusinessIfNull(
            fileInfo,
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "文件不存在"
        );
        // 仅正常状态的文件允许公开访问（已删除、待物理删除、审核中、违规下架均拒绝）
        ExceptionThrowerCore.throwBusinessIf(
            !FileStatusEnum.NORMAL.getValue().equals(fileInfo.getStatus()),
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "文件不可访问"
        );

        // 查询该文件关联的业务记录，用于后续鉴权
        List<FileBusinessInfo> refs = fileBusinessInfoRepository.listByFileId(
            fileId
        );
        if (refs != null && !refs.isEmpty()) {
            Long currentUserId = SecurityUtils.getUserId();
            // 文件上传者直接放行，无需检查关联文章权限
            boolean isUploader = refs.stream()
                .anyMatch(r -> currentUserId != null && currentUserId.equals(r.getUserId()));
            if (isUploader) {
                return buildFileContent(fileInfo);
            }
            String articleRefType =
                FileReferenceTypeEnum.ARTICLE_ATTACHMENT.getValue();
            // 只筛选关联了真实文章（referenceId > 0）的记录
            List<FileBusinessInfo> articleRefs = refs.stream()
                .filter(r -> articleRefType.equals(r.getReferenceType())
                    && r.getReferenceId() != null && r.getReferenceId() > 0)
                .toList();
            if (!articleRefs.isEmpty()) {
                boolean hasAccess = false;
                for (FileBusinessInfo ref : articleRefs) {
                    BlogArticle article = blogArticleRepository.getById(
                        ref.getReferenceId()
                    );
                    if (
                        article != null &&
                        articleAccessControlService.canAccessArticle(
                            article,
                            currentUserId
                        )
                    ) {
                        hasAccess = true;
                        break;
                    }
                }
                if (!hasAccess) {
                    ExceptionThrowerCore.throwBusinessEx(
                        ResultErrorCode.FORBIDDEN,
                        "当前用户无权访问该文件的关联文章"
                    );
                }
            }
        }

        return buildFileContent(fileInfo);
    }

    /**
     * 从存储服务获取文件内容并组装为 VO。
     */
    private FileContentVO buildFileContent(FileInfo fileInfo) {
        String objectName = resolveObjectName(fileInfo);
        try {
            InputStream inputStream = storageManager.download(objectName);
            ExceptionThrowerCore.throwBusinessIfNull(
                inputStream,
                ResultErrorCode.IO_ERROR,
                "文件读取失败"
            );
            String fileName =
                fileInfo.getOriginalName() != null
                    ? fileInfo.getOriginalName()
                    : fileInfo.getFileName();
            return new FileContentVO(
                inputStream,
                fileName,
                fileInfo.getMimeType(),
                fileInfo.getFileSize()
            );
        } catch (Exception e) {
            log.error(
                "文件读取失败: fileId={}, objectName={}",
                fileInfo.getId(),
                objectName,
                e
            );
            ExceptionThrowerCore.throwBusinessEx(
                ResultErrorCode.IO_ERROR,
                "文件读取失败"
            );
            return null;
        }
    }

    private String resolveObjectName(FileInfo fileInfo) {
        if (
            fileInfo.getFilePath() != null && !fileInfo.getFilePath().isBlank()
        ) {
            return fileInfo.getFilePath();
        }
        return fileInfo.getFileName();
    }
}
