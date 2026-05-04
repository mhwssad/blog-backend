package com.cybzacg.blogbackend.module.file.controller;

import com.cybzacg.blogbackend.module.file.model.data.FileContentVO;
import com.cybzacg.blogbackend.module.file.service.PublicFileAccessService;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
    private final PublicFileAccessService publicFileAccessService;

    @GetMapping("/{fileId}")
    @Operation(summary = "代理访问文件")
    public ResponseEntity<StreamingResponseBody> getFile(@PathVariable Long fileId) {
        FileContentVO content = publicFileAccessService.getFileContent(fileId);

        HttpHeaders headers = new HttpHeaders();
        String encodedFileName = URLEncoder.encode(content.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(encodedFileName, StandardCharsets.UTF_8)
                .build());

        if (content.getMimeType() != null && !content.getMimeType().isBlank()) {
            headers.setContentType(MediaType.parseMediaType(content.getMimeType()));
        }
        if (content.getContentLength() != null && content.getContentLength() >= 0) {
            headers.setContentLength(content.getContentLength());
        }

        StreamingResponseBody responseBody = outputStream -> {
            try (var inputStream = content.getContent()) {
                inputStream.transferTo(outputStream);
            } catch (IOException e) {
                log.error("文件流式响应失败: fileId={}", fileId, e);
                throw e;
            }
        };
        return ResponseEntity.ok().headers(headers).body(responseBody);
    }
}
