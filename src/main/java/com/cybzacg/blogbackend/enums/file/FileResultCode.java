package com.cybzacg.blogbackend.enums.file;

import com.cybzacg.blogbackend.enums.error.ResultCode;
import lombok.AllArgsConstructor;

/**
 * 文件模块错误码。
 */
@AllArgsConstructor
public enum FileResultCode implements ResultCode {
    STORAGE_NODE_NOT_CONFIGURED(71001, "未配置可用的存储节点"),
    UPLOAD_FILE_EMPTY(71002, "上传文件不能为空"),
    CHUNK_TASK_REQUIRED(71003, "当前任务为分片上传，请使用分片接口"),
    FILE_UPLOAD_FAILED(71004, "文件上传失败"),
    CHUNK_FILE_EMPTY(71005, "分片文件不能为空"),
    CHUNK_NUMBER_INVALID(71006, "分片序号非法"),
    NON_CHUNK_TASK(71007, "当前任务不是分片上传"),
    CHUNK_NUMBER_EXCEEDED(71008, "分片序号超过总分片数"),
    CHUNK_MD5_MISMATCH(71009, "分片MD5校验失败"),
    CHUNK_UPLOAD_FAILED(71010, "分片上传失败"),
    CHUNK_INCOMPLETE(71011, "分片未全部上传完成"),
    FILE_MD5_REQUIRED(71012, "缺少文件MD5"),
    CHUNK_MERGE_FAILED(71013, "分片合并失败"),
    FILE_REFERENCE_NOT_FOUND(71014, "文件引用不存在"),
    INIT_REQUEST_EMPTY(71015, "请求不能为空"),
    FILE_SIZE_INVALID(71016, "文件大小非法"),
    FILE_EXTENSION_NOT_ALLOWED(71017, "文件类型不允许"),
    UPLOAD_ID_REQUIRED(71018, "uploadId不能为空"),
    UPLOAD_TASK_NOT_FOUND(71019, "上传任务不存在"),
    STORAGE_NODE_UNAVAILABLE(71020, "存储节点不可用"),
    FILE_MD5_MISMATCH(71021, "文件MD5校验失败"),
    FILE_MD5_CALCULATE_FAILED(71022, "计算MD5失败"),
    FILE_NOT_FOUND(71023, "文件不存在"),
    FILE_STATUS_INVALID(71024, "文件状态非法"),
    UPLOAD_TASK_STATUS_INVALID(71025, "上传任务状态非法"),
    UPLOAD_TASK_EXPIRED(71026, "上传任务已过期");

    private final Integer code;
    private final String message;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
