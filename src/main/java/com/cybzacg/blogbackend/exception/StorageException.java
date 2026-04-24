package com.cybzacg.blogbackend.exception;

import com.cybzacg.blogbackend.enums.error.StorageResultCode;

/**
 * 存储模块运行时异常。<p>继承 BusinessException，专门用于存储服务（OSS、COS、MinIO、本地）相关的错误场景。</p>
 */
public class StorageException extends BusinessException {

    public StorageException(StorageResultCode resultCode) {
        super(resultCode);
    }

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(Integer code, String message) {
        super(code, message);
    }

    public StorageException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
