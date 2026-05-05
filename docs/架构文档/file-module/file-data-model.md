# File 数据模型

## 1. 数据库表结构

File 模块涉及 4 张核心表：

| 表名 | 说明 | 关联关系 |
|------|------|----------|
| `file_info` | 文件物理信息表 | 主表 |
| `file_upload_task` | 上传任务表 | 关联 file_info |
| `file_chunk` | 分片信息表 | 关联 file_upload_task |
| `file_business_info` | 业务引用表 | 关联 file_info |

## 2. 表详细设计

### 2.1 file_info（文件物理信息表）

```sql
CREATE TABLE file_info (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    upload_task_id  BIGINT COMMENT '关联上传任务ID',
    file_name       VARCHAR(255) COMMENT '存储文件名',
    original_name   VARCHAR(255) COMMENT '原始文件名',
    file_path       VARCHAR(500) COMMENT '文件存储路径',
    storage_key     VARCHAR(50) COMMENT '存储唯一键',
    file_url        VARCHAR(500) COMMENT '文件访问URL',
    file_size       BIGINT COMMENT '文件大小（字节）',
    file_type       VARCHAR(20) COMMENT '文件类型：image/document/video/audio/other',
    mime_type       VARCHAR(100) COMMENT 'MIME类型',
    file_extension  VARCHAR(20) COMMENT '文件扩展名',
    md5             VARCHAR(32) COMMENT '文件MD5哈希值',
    reference_count INT DEFAULT 0 COMMENT '业务引用计数',
    is_public       TINYINT DEFAULT 0 COMMENT '是否公开：0-私密，1-公开',
    category        VARCHAR(50) COMMENT '文件分类',
    download_count  INT DEFAULT 0 COMMENT '下载次数',
    upload_user_id  BIGINT COMMENT '上传用户ID',
    remark          VARCHAR(500) COMMENT '备注',
    status          TINYINT DEFAULT 1 COMMENT '文件状态：0-已删除，1-正常，2-待物理删除，3-审核中，4-违规下架',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_md5 (md5),
    INDEX idx_status (status),
    INDEX idx_upload_user_id (upload_user_id)
);
```

### 2.2 file_upload_task（上传任务表）

```sql
CREATE TABLE file_upload_task (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    upload_id         VARCHAR(32) COMMENT '上传唯一标识（UUID）',
    file_id           BIGINT COMMENT '关联文件ID（上传完成后回填）',
    upload_user_id    BIGINT COMMENT '上传用户ID',
    storage_key       VARCHAR(50) COMMENT '存储唯一键',
    source_ip         VARCHAR(50) COMMENT '来源IP地址',
    is_quick_upload   TINYINT DEFAULT 0 COMMENT '是否秒传：0-否，1-是',
    referenced_file_id BIGINT COMMENT '秒传引用的已有文件ID',
    file_md5          VARCHAR(32) COMMENT '文件MD5哈希值',
    file_size         BIGINT COMMENT '文件大小（字节）',
    original_name     VARCHAR(255) COMMENT '原始文件名',
    mime_type         VARCHAR(100) COMMENT 'MIME类型',
    reference_type    VARCHAR(50) COMMENT '业务引用类型',
    reference_id      BIGINT COMMENT '业务引用ID',
    category          VARCHAR(50) COMMENT '文件分类',
    is_public         TINYINT DEFAULT 0 COMMENT '是否公开：0-私密，1-公开',
    remark            VARCHAR(500) COMMENT '备注',
    is_chunked        TINYINT DEFAULT 0 COMMENT '是否分片上传：0-否，1-是',
    chunk_size        BIGINT COMMENT '分片大小（字节）',
    total_chunks      INT COMMENT '总分片数',
    uploaded_chunks   INT DEFAULT 0 COMMENT '已上传分片数',
    task_status       TINYINT DEFAULT 0 COMMENT '任务状态：0-待上传，1-上传中，2-已完成，3-失败，4-已过期',
    retry_count       INT DEFAULT 0 COMMENT '重试次数',
    start_time        DATETIME COMMENT '开始上传时间',
    complete_time     DATETIME COMMENT '完成时间',
    quick_upload_time DATETIME COMMENT '秒传完成时间',
    expire_time       DATETIME COMMENT '任务过期时间',
    error_code        VARCHAR(20) COMMENT '错误码',
    error_message     VARCHAR(500) COMMENT '错误信息',
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_upload_id (upload_id),
    INDEX idx_upload_user_id (upload_user_id),
    INDEX idx_task_status (task_status)
);
```

### 2.3 file_chunk（分片信息表）

```sql
CREATE TABLE file_chunk (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    upload_task_id BIGINT COMMENT '关联上传任务ID',
    chunk_number   INT COMMENT '分片序号（从1开始）',
    chunk_size     BIGINT COMMENT '分片大小（字节）',
    chunk_md5      VARCHAR(32) COMMENT '分片MD5哈希值',
    upload_status  TINYINT DEFAULT 0 COMMENT '上传状态：0-待上传，1-已上传',
    retry_count    INT DEFAULT 0 COMMENT '重试次数',
    upload_time    DATETIME COMMENT '上传完成时间',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_chunk (upload_task_id, chunk_number)
);
```

### 2.4 file_business_info（业务引用表）

```sql
CREATE TABLE file_business_info (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_id        BIGINT COMMENT '文件ID',
    user_id        BIGINT COMMENT '使用者ID',
    reference_type VARCHAR(50) COMMENT '业务引用类型：avatar/chat_message/article_attachment/temp',
    reference_id   BIGINT COMMENT '业务引用ID',
    source_ip      VARCHAR(50) COMMENT '来源IP地址',
    is_public      TINYINT DEFAULT 0 COMMENT '是否公开：0-私密，1-公开',
    category       VARCHAR(50) COMMENT '文件分类',
    remark         VARCHAR(500) COMMENT '备注',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_file_user_ref (file_id, user_id, reference_type, reference_id),
    INDEX idx_user_id (user_id),
    INDEX idx_reference (reference_type, reference_id)
);
```

## 3. 实体类映射

| 实体类 | 表名 | 说明 |
|--------|------|------|
| `FileInfo` | file_info | 文件物理信息 |
| `FileUploadTask` | file_upload_task | 上传任务 |
| `FileChunk` | file_chunk | 分片信息 |
| `FileBusinessInfo` | file_business_info | 业务引用 |

## 4. 核心枚举

### 4.1 FileStatusEnum（文件状态）

```java
public enum FileStatusEnum {
    DELETED(0, "已删除"),
    NORMAL(1, "正常"),
    PHYSICAL_DELETE_PENDING(2, "待物理删除"),
    REVIEWING(3, "审核中"),
    VIOLATION(4, "违规下架");
}
```

### 4.2 FileReferenceTypeEnum（业务引用类型）

```java
public enum FileReferenceTypeEnum {
    AVATAR("avatar", "头像"),
    CHAT_MESSAGE("chat_message", "聊天消息文件"),
    ARTICLE_ATTACHMENT("article_attachment", "文章附件"),
    TEMP("temp", "临时文件");
}
```

### 4.3 FileCategoryEnum（文件分类）

```java
public enum FileCategoryEnum {
    IMAGE("image", "图片"),
    DOCUMENT("document", "文档"),
    VIDEO("video", "视频"),
    AUDIO("audio", "音频"),
    OTHER("other", "其他");
}
```

### 4.4 FileResultCode（错误码）

| 错误码 | 说明 |
|--------|------|
| 71001 | 未配置可用的存储节点 |
| 71002 | 上传文件不能为空 |
| 71003 | 当前任务为分片上传，请使用分片接口 |
| 71004 | 文件上传失败 |
| 71005 | 分片文件不能为空 |
| 71006 | 分片序号非法 |
| 71007 | 当前任务不是分片上传 |
| 71008 | 分片序号超过总分片数 |
| 71009 | 分片MD5校验失败 |
| 71010 | 分片上传失败 |
| 71011 | 分片未全部上传完成 |
| 71012 | 缺少文件MD5 |
| 71013 | 分片合并失败 |
| 71014 | 文件引用不存在 |
| 71015 | 请求不能为空 |
| 71016 | 文件大小非法 |
| 71017 | 文件类型不允许 |
| 71018 | uploadId不能为空 |
| 71019 | 上传任务不存在 |
| 71020 | 存储节点不可用 |
| 71021 | 文件MD5校验失败 |
| 71022 | 计算MD5失败 |
| 71023 | 文件不存在 |
| 71024 | 文件状态非法 |
| 71025 | 上传任务状态非法 |
| 71026 | 上传任务已过期 |

## 5. Repository 接口

| Repository | 职责 |
|------------|------|
| `FileInfoRepository` | 文件物理信息持久化：秒传检测、后台分页、引用数刷新 |
| `FileUploadTaskRepository` | 任务持久化：按用户/状态分页、过期任务查询 |
| `FileChunkRepository` | 分片持久化：分片记录增删改查 |
| `FileBusinessInfoRepository` | 业务引用持久化：引用计数、绑定/解绑 |