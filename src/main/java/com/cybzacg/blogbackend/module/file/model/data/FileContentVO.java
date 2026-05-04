package com.cybzacg.blogbackend.module.file.model.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;

/**
 * 文件内容传输对象。
 * 承载文件下载所需的内容流、文件名、MIME 类型和内容长度。
 */
@Data
@AllArgsConstructor
public class FileContentVO {
    /**
     * 文件内容流
     */
    private InputStream content;
    /**
     * 文件名（优先使用原始文件名）
     */
    private String fileName;
    /**
     * MIME 类型
     */
    private String mimeType;
    /**
     * 文件大小（字节）
     */
    private Long contentLength;
}
