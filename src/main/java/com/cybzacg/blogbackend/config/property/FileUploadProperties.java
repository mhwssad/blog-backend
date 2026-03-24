package com.cybzacg.blogbackend.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传属性配置
 *
 * @author system
 * @since 2025/12/28
 */
@Component
@ConfigurationProperties(prefix = "file-upload")
@Data
public class FileUploadProperties {

    /**
     * 分片大小，默认5MB
     */
    private Long chunkSize = 5242880L;

    /**
     * 分片大小阈值，超过此值强制分片，默认6MB
     */
    private Long chunkSizeThreshold = 6291456L;

    /**
     * 任务过期天数，默认2天
     */
    private Integer taskExpireDays = 2;

    /**
     * 允许的文件扩展名列表，空表示不限制
     */
    private List<String> allowedExtensions = new ArrayList<>();

    /**
     * 最大文件大小，默认100MB
     */
    private Long maxFileSize = 104857600L;

    /**
     * 临时文件存储目录前缀
     */
    private String tempDirPrefix = "temp";

    /**
     * 是否启用MD5校验，默认启用
     */
    private Boolean enableMd5Check = true;
}

