package com.cybzacg.blogbackend.enums.error;

/**
 * 存储模块错误码。
 */
public enum StorageResultCode implements ResultCode {
    UPLOAD_FAILED(70001, "上传失败"),
    GET_FILE_STREAM_FAILED(70002, "获取文件流失败"),
    DELETE_FAILED(70003, "删除失败"),
    BATCH_DELETE_FAILED(70004, "批量删除失败"),
    FILE_NOT_EXIST(70005, "文件不存在"),
    GET_DOWNLOAD_URL_FAILED(70006, "获取下载地址失败"),
    UPLOAD_MERGE_FILES(70007, "合并文件失败");

    private final Integer code;
    private final String message;

    StorageResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
