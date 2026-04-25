# 文件模块数据库评审与业务建设计划

## 1. 背景

当前项目已经具备文件存储基础设施和多存储节点管理能力，但文件业务层仍未落地：

- 已有数据库脚本：`src/main/resources/mysql/04_file.sql`
- 已有底层存储能力：`StorageService`、`StorageManager`
- 已有上传配置能力：`FileUploadProperties`
- 尚未实现：
  - 文件实体、Mapper、Service、Controller
  - 文件业务接口
  - 文件模块 API 文档
  - 文件与文章/评论/头像等业务对象的正式引用规范

本计划用于统一文件模块的数据口径、业务边界和实施顺序，为后续代码实现提供直接可执行的规范。

## 2. 建设目标

- 评审并修订文件模块数据库设计
- 建立“物理文件 + 上传任务 + 分片过程 + 业务引用”四层模型
- 实现后台管理与登录用户上传的最小完整闭环
- 基于现有 `StorageManager` 落地文件上传、秒传、分片上传和删除治理
- 补充文件模块 API 文档与开发验收基线

## 3. 当前现状分析

### 3.1 数据库现状

当前文件模块包含 4 张表：

- `file_info`：物理文件主表
- `file_upload_task`：上传任务表
- `file_chunk`：分片元数据表
- `file_business_info`：业务引用关系表

### 3.2 代码现状

已具备以下能力：

- `StorageService`
  - 上传
  - 下载
  - 删除
  - 获取访问地址
  - 上传临时文件
  - 合并分片
  - 删除临时目录
- `StorageManager`
  - 多存储节点统一入口
  - 健康检查
  - 故障转移
  - 负载均衡
  - 节点切换
- `FileUploadProperties`
  - 分片大小
  - 分片阈值
  - 任务过期时间
  - 扩展名白名单
  - 文件体积限制
  - 临时目录前缀
  - MD5 校验开关

### 3.3 当前缺失

- 文件表对应 Java 实体
- 文件表对应 Mapper / XML 或查询封装
- 用户上传业务服务
- 后台文件管理服务
- 文件模块控制器
- 文件模块接口文档
- 文件权限点和初始化菜单
- 开发环境的存储配置闭环

## 4. 数据模型口径

### 4.1 `file_info`

定位：物理文件表

- 一条记录代表一个去重后的物理文件对象
- 与存储系统中的单个对象一一对应
- 负责存储路径、校验信息、状态、可见性、上传用户等物理层属性

核心原则：

- 允许通过 MD5 复用物理文件
- 物理文件是否仍被使用，不由上传任务决定，而由业务引用决定
- 每个物理文件必须记录实际落在哪个存储节点，不能只依赖当前活跃节点

### 4.2 `file_upload_task`

定位：上传生命周期表

- 一条记录表示一次上传任务
- 支持普通上传、秒传、分片上传
- 任务可以在失败、取消、超时场景下独立存在
- 每个任务必须绑定一个确定的 `storage_key`，避免多节点负载均衡导致分片落到不同节点

### 4.3 `file_chunk`

定位：分片过程表

- 一条记录表示上传任务中的一个分片
- 只服务于上传过程，不承载文件最终语义
- 分片对象名统一通过上传任务和分片序号推导

### 4.4 `file_business_info`

定位：业务引用关系表

- 一条记录表示某个业务对象对某个文件的一次引用
- 一个物理文件可被多个业务对象引用
- 删除业务时优先解除引用，再判断物理文件是否需要回收

## 5. 数据库评审结论与调整方案

### 5.1 `file_info`

合理点：

- 文件元数据字段完整
- 已有大小、类型、扩展名、MD5、上传用户、状态等基础字段
- 已有 `reference_count`，适合做引用治理

问题：

- 缺少 `storage_key`，无法在多存储节点模式下准确删除和读取文件
- `upload_task_id` 设为 `NOT NULL` 过于刚性，不利于后续离线导入或系统补录
- `category` 为自由文本，缺少枚举边界

调整：

- 新增 `storage_key`
- `upload_task_id` 改为可空
- 保留 `uk_md5` 作为 v1 物理去重策略
- `category` 仅允许受控值

### 5.2 `file_upload_task`

合理点：

- 已覆盖秒传、分片、任务状态、重试、过期和审计字段
- 与现有 `TaskStatusEnum` 兼容

问题：

- 缺少 `storage_key`，无法保证同一任务固定落在同一存储节点
- `idx_md5_user` 偏向用户内秒传，但物理去重是全局口径

调整：

- 新增 `storage_key`
- 秒传命中查询优先查询 `file_info`
- `uploaded_chunks` 作为性能字段保留，但真实进度以 `file_chunk` 为准

### 5.3 `file_chunk`

合理点：

- `(upload_task_id, chunk_number)` 唯一约束合理
- 支持记录状态、大小、重试和时间

问题：

- 未明确分片对象路径策略

调整：

- 统一约定分片对象名：`temp/{uploadId}/chunk-{chunkNumber}.part`
- 不额外增加分片路径字段，避免与对象命名策略重复

### 5.4 `file_business_info`

合理点：

- 已识别“物理文件”和“业务关系”需要分层

问题：

- `uk_file_id` 会导致一个文件只能有一条业务记录，不符合引用表定位
- `file_name`、`file_url`、`is_public`、`category` 与 `file_info` 存在重复表达
- `category` 类型与 `file_info.category` 不一致

调整：

- 删除 `uk_file_id`
- 改为 `(file_id, reference_type, reference_id)` 唯一
- 删除重复字段，仅保留业务关系需要的字段
- `category` 改为 `VARCHAR(32)`，与主表口径一致

## 6. 首期业务范围

### 6.1 包含范围

- 登录用户上传文件
- 秒传
- 普通上传
- 分片上传
- 用户查询自己的文件和任务
- 用户删除自己的文件引用
- 后台分页查询文件和上传任务
- 后台修改文件状态
- 后台强制删除文件

### 6.2 不包含范围

- 匿名公开上传
- 文件分享
- 图片裁剪和压缩
- 音视频转码
- 在线预览转换
- 独立 CDN 刷新和回源治理

## 7. 业务约定

### 7.1 文件状态

- `0`：已删除
- `1`：正常
- `2`：审核中
- `3`：违规下架

### 7.2 上传任务状态

- `0`：初始化
- `1`：上传中
- `2`：合并中
- `3`：已完成
- `4`：失败
- `5`：已取消

### 7.3 业务引用类型

首批受控值：

- `avatar`
- `article_attachment`
- `comment_image`
- `temp`

### 7.4 文件分类

首批受控值：

- `avatar`
- `attachment`
- `comment`
- `temp`

## 8. 模块设计

### 8.1 包结构

新增 `module/file`：

- `module/file/controller`
- `module/file/service`
- `module/file/service/impl`
- `module/file/model/user`
- `module/file/model/admin`

同时补齐：

- `domain`
- `mapper`
- `resources/com/cybzacg/blogbackend/mapper`

### 8.2 基础服务

- `FileInfoService`
- `FileUploadTaskService`
- `FileChunkService`
- `FileBusinessInfoService`

### 8.3 业务服务

- `UserFileService`
- `FileAdminService`

## 9. 用户侧业务流程

### 9.1 初始化上传任务

输入：

- 原始文件名
- 文件大小
- 文件 MD5
- MIME 类型
- 引用类型
- 引用对象 ID
- 业务分类
- 是否公开

处理：

- 校验大小与扩展名
- 计算上传模式
- 绑定当前 `storage_key`
- 创建 `file_upload_task`
- 若已命中可复用文件，返回可秒传标记

输出：

- `uploadId`
- 上传模式
- 是否可秒传
- 分片参数

### 9.2 秒传

处理：

- 根据任务 MD5 查询 `file_info`
- 若存在物理文件：
  - 创建业务引用
  - 增加 `reference_count`
  - 更新任务状态为完成
- 若不存在：
  - 返回未命中

### 9.3 普通上传

处理：

- 上传二进制到任务绑定的存储节点
- 根据 MD5 判重
- 若物理文件已存在则复用，删除刚上传文件或直接跳过上传
- 若不存在则创建 `file_info`
- 创建业务引用
- 任务置为完成

### 9.4 分片上传

处理：

- 分片上传至 `temp/{uploadId}/chunk-{chunkNumber}.part`
- 更新 `file_chunk`
- 更新 `uploaded_chunks`
- 完成时调用合并
- 合并成功后创建 `file_info`
- 创建业务引用
- 清理临时目录

### 9.5 用户查询

- 查询我的文件列表
- 查询我的上传任务列表
- 支持按分类、引用类型、状态筛选

### 9.6 用户删除

处理：

- 删除业务引用
- 递减 `reference_count`
- 若引用数归零：
  - 删除物理对象
  - 文件状态置为已删除

## 10. 后台侧业务流程

### 10.1 文件分页

支持条件：

- 用户 ID
- 状态
- 分类
- 是否公开
- 引用类型
- 文件名关键字

### 10.2 任务分页

支持条件：

- 上传用户
- 任务状态
- 是否秒传
- 是否分片
- 上传时间

### 10.3 文件详情

展示内容：

- 物理文件信息
- 引用列表
- 最近任务列表

### 10.4 状态治理

- 正常
- 审核中
- 违规下架
- 已删除

### 10.5 强制删除

处理：

- 删除业务引用
- 删除物理文件
- 清理任务和分片关联数据
- 文件状态改为已删除

## 11. API 规划

### 11.1 用户接口

- `POST /api/user/files/upload-tasks/init`
- `POST /api/user/files/upload-tasks/{uploadId}/quick-check`
- `POST /api/user/files/upload-tasks/{uploadId}/file`
- `POST /api/user/files/upload-tasks/{uploadId}/chunks/{chunkNumber}`
- `POST /api/user/files/upload-tasks/{uploadId}/complete`
- `GET /api/user/files`
- `GET /api/user/files/upload-tasks`
- `DELETE /api/user/files/{businessId}`

### 11.2 后台接口

- `GET /api/sys/files`
- `GET /api/sys/files/{id}`
- `GET /api/sys/files/upload-tasks`
- `PUT /api/sys/files/{id}/status`
- `DELETE /api/sys/files/{id}`

## 12. 存储实现约定

- 所有上传任务初始化时固定一个 `storage_key`
- 同一任务的普通上传、分片上传、合并、临时目录删除都必须使用该节点
- `file_info.storage_key` 必须保存最终文件所在节点
- 删除和读取文件时优先按 `storage_key` 精准路由到对应节点
- `file_path` 存对象键，不存绝对本地路径
- `file_url` 存可直接访问地址或本地开发地址

## 13. 代码实施顺序

1. 创建计划文档
2. 修订 `04_file.sql`
3. 补齐实体、Mapper、基础 Service
4. 实现用户侧上传链路
5. 实现后台管理链路
6. 补权限初始化 SQL
7. 补 API 文档
8. 补测试并编译验证

## 14. 测试计划

### 14.1 数据层

- 同一文件允许多个业务引用
- 删除业务引用后引用计数正确回退
- 任务和分片状态能正确写入

### 14.2 业务层

- 秒传命中
- 秒传未命中
- 普通上传成功
- 分片上传成功
- 合并失败回滚任务状态
- 删除最后一个引用后物理文件被回收

### 14.3 接口层

- 用户文件分页返回统一结构
- 用户初始化上传任务返回上传模式
- 后台文件分页返回统一结构
- 后台状态修改返回成功

## 15. 验收标准

- 文件模块完成从表结构到控制器的最小完整闭环
- 用户可以完成初始化、上传、查询、删除
- 后台可以完成查询、详情、状态修改、删除
- 文档与代码保持一致
- 编译通过
