package com.cybzacg.blogbackend.config.property;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * 默认使用的存储类型如： oss， cos， local， minio等， 后续可扩展
     */
    @NotBlank(message = "存储类型不能为空，支持oss、cos、local、minio")
    @Pattern(regexp = "^(oss|cos|local|minio)$", message = "存储类型仅支持oss、cos、local、minio")
    private String storageType;

    /**
     * 多存储节点配置列表（支持多个不同/相同类型的存储节点）
     */
    @Valid // 关键：启用列表中每个嵌套对象的校验
    @NestedConfigurationProperty
    @NotEmpty(message = "存储节点配置列表不能为空")
    private List<Storage> storage;

    @Data
    @Validated
    public static class Storage {

        /**
         * 当前节点的存储类型（oss/cos/local/minio）
         */
        @NotBlank(message = "存储节点类型不能为空")
        @Pattern(regexp = "^(oss|cos|local|minio)$", message = "存储节点类型仅支持oss、cos、local、minio")
        private String type;

        /**
         * 节点标识（自定义名称，用于区分同类型的多个节点，如oss-1、oss-2）
         */
        @NotBlank(message = "存储节点标识不能为空")
        private String key;

        /**
         * 基础访问URL（如OSS的CDN地址、本地文件的访问前缀）
         */
        @URL(message = "基础URL格式不正确")
        private String baseUrl;

        /**
         * 服务端点地址
         * - oss：阿里云OSS的Endpoint（如oss-cn-hangzhou.aliyuncs.com）
         * - cos：腾讯云COS的Endpoint（如cos.ap-guangzhou.myqcloud.com）
         * - minio：MinIO服务端点地址（如http://localhost:9000）
         * - local：无需填写（自动忽略校验）
         */
        private String endpoint;

        /**
         * 访问凭据
         */
        private String accessKey;

        /**
         * 凭据密钥
         */
        private String accessKeySecret;

        /**
         * 存储桶名称/本地存储路径
         * - oss/cos/minio：存储桶名称
         * - local：本地文件存储的根路径（如/opt/storage或D:/storage）
         */
        @NotBlank(message = "存储桶/本地路径不能为空")
        private String bucketName;
    }
}

