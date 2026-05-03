package com.cybzacg.blogbackend.module.file.model.data;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件内容传输对象。
 * 承载文件下载所需的字节内容、文件名和 MIME 类型。
 */
@Data
@AllArgsConstructor
public class FileContentVO {
    /**
     * 文件字节内容
     */
    private byte[] content;
    /**
     * 文件名（优先使用原始文件名）
     */
    private String fileName;
    /**
     * MIME 类型
     */
    private String mimeType;
}
