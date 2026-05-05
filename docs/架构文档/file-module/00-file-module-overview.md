# File 模块总览

## 1. 模块定位

File 模块（`module/file`）是博客后端的核心文件服务层，负责：

- **文件上传**：支持普通上传、分片上传、秒传三种模式
- **文件存储**：多存储节点支持（本地、MinIO、OSS）
- **文件生命周期**：引用计数、状态流转、物理删除
- **公开访问代理**：带文章权限校验的文件访问
- **聊天文件门面**：文件与聊天消息的绑定/解绑

## 2. 架构分层

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户侧接口                               │
│   POST /api/user/files/upload-tasks/init    GET /api/user/files  │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller 层                              │
│   UserFileController        PublicFileAccessController         │
│   FileAdminController                                        │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service 层                                 │
│  ┌──────────────────┐  ┌──────────────────┐                    │
│  │ UserFileService  │  │ UserFileQueryService│                  │
│  │ 用户上传门面      │  │ 用户文件查询       │                    │
│  └──────────────────┘  └──────────────────┘                    │
│  ┌──────────────────┐  ┌──────────────────┐                    │
│  │ FileUploadService│  │ FileLifecycleService│                  │
│  │ 分片上传/合并     │  │ 生命周期收口       │                    │
│  └──────────────────┘  └──────────────────┘                    │
│  ┌──────────────────┐  ┌──────────────────┐                    │
│  │ FileAdminService │  │ FileChatFacadeService│                  │
│  │ 后台管理         │  │ 文件-聊天门面      │                    │
│  └──────────────────┘  └──────────────────┘                    │
└─────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   Repository    │   │  StorageManager │   │  定时任务        │
│   数据访问       │   │  多节点存储      │   │  清理/重试      │
└─────────────────┘   └─────────────────┘   └─────────────────┘
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  MySQL          │   │  Local/Minio/OSS│   │  分布式调度      │
│  持久化存储       │   │  文件物理存储    │   │  过期回收       │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

## 3. 目录结构

```
module/file/
├── controller/          # 控制器
│   ├── UserFileController.java          # 用户文件上传/管理
│   ├── FileAdminController.java         # 后台文件管理
│   └── PublicFileAccessController.java  # 公开文件访问代理
├── convert/            # MapStruct 转换器
│   ├── FileModelConvert.java           # 模型转换
│   └── FileUploadConvert.java           # 上传相关转换
├── model/              # 请求/响应模型
│   ├── admin/          # 后台管理请求/VO
│   ├── user/           # 用户侧请求/VO
│   └── data/           # 内部数据传输对象
├── repository/         # 数据访问层
│   ├── FileInfoRepository.java
│   ├── FileUploadTaskRepository.java
│   ├── FileChunkRepository.java
│   ├── FileBusinessInfoRepository.java
│   └── impl/           # MyBatis-Plus 实现
├── service/            # 业务服务
│   ├── UserFileService.java
│   ├── UserFileQueryService.java
│   ├── FileUploadService.java
│   ├── FileLifecycleService.java
│   ├── FileAdminService.java
│   ├── FileChatFacadeService.java
│   ├── PublicFileAccessService.java
│   └── impl/           # 服务实现
└── task/               # 定时任务
    ├── FileUploadTaskCleanupScheduler.java      # 过期任务清理
    └── FilePhysicalDeleteRetryScheduler.java   # 物理删除重试
```

## 4. 数据库表结构

| 表名 | 说明 |
|------|------|
| `file_info` | 文件物理信息表：存储文件元数据、存储路径、状态 |
| `file_upload_task` | 上传任务表：跟踪上传进度、分片状态、过期时间 |
| `file_chunk` | 分片信息表：记录各分片的上传状态、MD5 |
| `file_business_info` | 业务引用表：文件与业务（用户、文章、聊天消息）的关联 |

## 5. 核心能力矩阵

| 能力 | 组件 | 状态 |
|------|------|------|
| 普通文件上传 | FileUploadService | ✅ 完成 |
| 分片上传 | FileUploadService | ✅ 完成 |
| 秒传 | FileUploadService | ✅ 完成 |
| 多存储节点 | StorageManager | ✅ 完成 |
| 文件生命周期 | FileLifecycleService | ✅ 完成 |
| 引用计数 | FileLifecycleService | ✅ 完成 |
| 公开访问代理 | PublicFileAccessService | ✅ 完成 |
| 后台文件管理 | FileAdminService | ✅ 完成 |
| 聊天文件门面 | FileChatFacadeService | ✅ 完成 |
| 过期任务清理 | FileUploadTaskCleanupScheduler | ✅ 完成 |
| 物理删除重试 | FilePhysicalDeleteRetryScheduler | ✅ 完成 |

## 6. 核心流程概览

### 6.1 文件上传流程

```
1. initUploadTask    → 创建上传任务（普通/分片模式）
2. quickCheck        → 秒传检测（MD5 命中则直接完成）
3. uploadFile        → 普通模式整文件上传
4. uploadChunk       → 分片模式逐分片上传
5. completeUpload   → 分片合并，触发引用创建
```

### 6.2 文件生命周期

```
上传完成 → NORMAL（正常）
    ↓ 引用归零
PHYSICAL_DELETE_PENDING（待物理删除）
    ↓ 删除成功
DELETED（已删除）
```

### 6.3 定时清理

- 每小时：清理过期未完成的上传任务
- 每30分钟：重试物理删除待处理的文件

## 7. 相关文档

- [文件上传流程](./file-upload-flow.md)
- [文件生命周期管理](./file-lifecycle-management.md)
- [文件数据模型](./file-data-model.md)
- [文件-聊天门面](./file-chat-facade.md)