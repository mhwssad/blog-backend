package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.file.model.data.FileContentVO;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.service.PublicFileAccessService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            boolean hasAccess = false;
            String articleRefType =
                FileReferenceTypeEnum.ARTICLE_ATTACHMENT.getValue();
            // 遍历关联文章，检查用户是否有文章访问权限
            for (FileBusinessInfo ref : refs) {
                if (
                    !articleRefType.equals(ref.getReferenceType()) ||
                    ref.getReferenceId() == null
                ) {
                    continue;
                }
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

            // 无任何文章引用授予访问权限时，拒绝访问
            if (!hasAccess) {
                boolean hasArticleRefs = refs
                    .stream()
                    .anyMatch(r -> articleRefType.equals(r.getReferenceType()));
                if (hasArticleRefs) {
                    ExceptionThrowerCore.throwBusinessEx(
                        ResultErrorCode.FORBIDDEN,
                        "当前用户无权访问该文件的关联文章"
                    );
                }
            }
        }

        // 解析对象名用于存储下载
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
                fileId,
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
