package com.cybzacg.blogbackend.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResultErrorCode implements ResultCode {
    SUCCESS(200, "成功"),
    FAIL(500, "操作失败"),

    PARAM_VALIDATION_FAILED(40001, "参数校验失败"),
    PARAM_BIND_ERROR(40002, "参数绑定失败"),
    PARAM_TYPE_MISMATCH(40003, "参数类型不匹配"),
    PARAM_FORMAT_INVALID(40004, "参数格式无效"),
    HTTP_MESSAGE_NOT_READABLE(40005, "请求体不可读"),
    MISSING_REQUEST_PARAMETER(40006, "缺少请求参数"),
    MISSING_REQUEST_HEADER(40007, "缺少请求头"),
    MISSING_PATH_VARIABLE(40008, "缺少路径变量"),
    MISSING_REQUEST_PART(40009, "缺少请求体部分"),
    MAX_UPLOAD_SIZE_EXCEEDED(40010, "上传文件超出大小限制"),
    ILLEGAL_ARGUMENT(40011, "非法参数"),
    HTTP_METHOD_NOT_SUPPORTED(40500, "不支持的请求方法"),
    FORBIDDEN(40300, "没有访问权限"),
    HTTP_MEDIA_TYPE_NOT_SUPPORTED(41500, "不支持的媒体类型"),
    NO_HANDLER_FOUND(40400, "请求的接口不存在"),
    USER_NOT_FOUND(40401, "用户不存在"),

    AUTH_FAILED(40100, "认证失败"),
    INVALID_CREDENTIALS(40101, "用户名或密码错误"),
    LOGIN_REQUIRED(40102, "未登录或登录已过期"),
    ACCOUNT_EXPIRED(40103, "账号已过期"),
    ACCOUNT_LOCKED(40104, "账号已锁定"),
    ACCOUNT_DISABLED(40105, "账号已禁用"),
    CREDENTIALS_EXPIRED(40106, "凭证已过期"),
    SESSION_EXPIRED(40107, "会话已失效"),
    INVALID_TOKEN(40108, "无效的令牌"),
    REMEMBER_ME_AUTH_FAILED(40109, "记住我认证失败"),
    COOKIE_THEFT_DETECTED(40110, "检测到Cookie被盗用，已清除相关会话"),
    INVALID_REMEMBER_ME_COOKIE(40111, "无效的Remember-Me Cookie"),
    EMAIL_CAPTCHA_INVALID(40112, "邮箱验证码错误"),
    EMAIL_CAPTCHA_EXPIRED(40113, "邮箱验证码已过期"),
    EMAIL_CAPTCHA_SEND_FAILED(40114, "邮箱验证码发送失败"),
    CSRF_TOKEN_VALIDATION_FAILED(40301, "CSRF令牌校验失败"),
    CSRF_TOKEN_MISSING(40302, "缺少CSRF令牌"),
    CSRF_TOKEN_INVALID(40303, "无效的CSRF令牌"),

    SYSTEM_ERROR(50000, "系统异常，请联系管理员"),
    CONCURRENT_MODIFICATION(50001, "并发修改异常"),
    NULL_POINTER_ERROR(50002, "空指针异常"),
    CLASS_CAST_ERROR(50003, "类型转换异常"),
    ARRAY_INDEX_OUT_OF_BOUNDS(50004, "数组越界异常"),
    ARITHMETIC_ERROR(50005, "算术异常"),
    ILLEGAL_STATE(50006, "非法状态异常"),
    IO_ERROR(50007, "IO异常"),
    JSON_PROCESSING_ERROR(50008, "JSON处理异常"),
    DATE_FORMAT_ERROR(50009, "日期格式错误"),
    NUMBER_FORMAT_ERROR(50010, "数字格式错误"),
    UNSUPPORTED_OPERATION(50011, "不支持的操作"),
    CLASS_NOT_FOUND(50012, "类未找到"),
    NO_SUCH_METHOD(50013, "方法未找到"),
    NO_SUCH_FIELD(50014, "字段未找到"),
    ILLEGAL_ACCESS(50015, "非法访问异常"),
    INSTANTIATION_ERROR(50016, "实例化异常"),
    INVOCATION_TARGET_ERROR(50017, "反射调用异常"),
    THREAD_INTERRUPTED(50018, "线程中断异常"),
    EXECUTION_ERROR(50019, "执行异常"),
    TIMEOUT_ERROR(50020, "超时异常"),

    DB_SQL_EXECUTION_ERROR(60001, "SQL执行异常"),
    DATA_ACCESS_ERROR(60002, "数据库访问异常"),
    DB_OPTIMISTIC_LOCK_FAILURE(60003, "乐观锁冲突"),
    DB_SQL_SYNTAX_ERROR(60004, "SQL语法错误"),
    DATA_INTEGRITY_VIOLATION(60005, "数据完整性校验失败"),
    DATA_ALREADY_EXISTS(60006, "数据已存在"),
    DB_FOREIGN_KEY_VIOLATION(60007, "外键约束冲突"),
    DB_PRIMARY_KEY_CONFLICT(60008, "主键冲突"),
    DB_UNIQUE_CONSTRAINT_VIOLATION(60009, "唯一约束冲突"),
    DB_SQL_TIMEOUT(60010, "SQL执行超时"),
    DB_CONNECTION_FAILED(60011, "数据库连接失败"),
    DB_DATA_RETRIEVAL_FAILED(60012, "数据检索失败"),
    DB_NOT_NULL_VIOLATION(60013, "非空约束冲突"),
    DB_PESSIMISTIC_LOCK_FAILURE(60014, "悲观锁获取失败"),
    DB_DEADLOCK_DETECTED(60015, "检测到数据库死锁"),
    DB_TRANSACTION_FAILED(60016, "事务处理失败"),
    DB_MAPPING_ERROR(60017, "数据库映射异常"),
    DB_INVALID_RESOURCE_USAGE(60018, "无效的数据访问资源使用"),
    DB_NON_TRANSIENT_ERROR(60019, "非暂时性数据访问异常"),
    DB_TRANSIENT_ERROR(60020, "暂时性数据访问异常");

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
