package com.cybzacg.blogbackend.module.file.controller;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 公开文件访问代理控制器。
 *
 * <p>代理文件下载并对关联文章做访问控制校验，防止私密/白名单文章附件被绕过文章权限直接访问。
 */
@Slf4j
@RestController
@RequestMapping("/api/public/files")
@Tag(name = "公开文件访问")
@RequiredArgsConstructor
public class PublicFileAccessController {
    private final FileInfoRepository fileInfoRepository;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final BlogArticleRepository blogArticleRepository;
    private final ArticleAccessControlService articleAccessControlService;
    private final StorageManager storageManager;

    @GetMapping("/{fileId}")
    @Operation(summary = "代理访问文件")
    public ResponseEntity<byte[]> getFile(@PathVariable Long fileId) {
        log.info("开始代理访问文件, fileId={}", fileId);
        FileInfo fileInfo = fileInfoRepository.getById(fileId);
        ExceptionThrowerCore.throwBusinessIfNull(fileInfo, ResultErrorCode.ILLEGAL_ARGUMENT, "文件不存在");
        ExceptionThrowerCore.throwBusinessIf(
                fileInfo.getStatus() != null && fileInfo.getStatus() == 1,
                ResultErrorCode.ILLEGAL_ARGUMENT, "文件已删除");
        // 查询该文件关联的业务记录，用于后续鉴权
        List<FileBusinessInfo> refs = fileBusinessInfoRepository.listByFileId(fileId);
        if (refs != null && !refs.isEmpty()) {
            Long currentUserId = SecurityUtils.getUserId();
            boolean hasAccess = false;
            // 遍历关联文章，检查用户是否有文章访问权限
            for (FileBusinessInfo ref : refs) {
                if (!"article_attachment".equals(ref.getReferenceType()) || ref.getReferenceId() == null) {
                    continue;
                }
                BlogArticle article = blogArticleRepository.getById(ref.getReferenceId());
                if (article != null && articleAccessControlService.canAccessArticle(article, currentUserId)) {
                    hasAccess = true;
                    break;
                }
            }

            // 无任何文章引用授予访问权限时，拒绝访问
            if (!hasAccess) {
                boolean hasArticleRefs = refs.stream()
                        .anyMatch(r -> "article_attachment".equals(r.getReferenceType()));
                if (hasArticleRefs) {
                    ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.FORBIDDEN, "当前用户无权访问该文件的关联文章");
                }
            }
        }

        // 解析对象名用于存储下载
        String objectName = resolveObjectName(fileInfo);
        try (InputStream inputStream = storageManager.download(objectName)) {
            byte[] bytes = inputStream.readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            String encodedFileName = URLEncoder.encode(
                    fileInfo.getOriginalName() != null ? fileInfo.getOriginalName() : fileInfo.getFileName(),
                    StandardCharsets.UTF_8).replace("+", "%20");
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename(encodedFileName, StandardCharsets.UTF_8)
                    .build());

            String mimeType = fileInfo.getMimeType();
            if (mimeType != null && !mimeType.isBlank()) {
                headers.setContentType(MediaType.parseMediaType(mimeType));
            }

            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (IOException e) {
            log.error("文件读取失败: fileId={}, objectName={}", fileId, objectName, e);
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.IO_ERROR, "文件读取失败");
            return null;
        }
    }

    private String resolveObjectName(FileInfo fileInfo) {
        if (fileInfo.getStorageKey() != null && !fileInfo.getStorageKey().isBlank()) {
            return fileInfo.getStorageKey();
        }
        if (fileInfo.getFilePath() != null && !fileInfo.getFilePath().isBlank()) {
            return fileInfo.getFilePath();
        }
        return fileInfo.getFileName();
    }
}
