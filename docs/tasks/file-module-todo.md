# File 模块待办清单

本文档用于收口 file 模块接下来要持续推进的任务，避免"知道有缺口，但每次都重新梳理一遍"。本清单按 2026-03-31 当前代码状态整理。

## 1. 模块结构概览

```
module/file/
├── controller/
│   ├── UserFileController.java        (8个端点: 初始化/秒传检测/整文件/分片/完成/文件列表/任务列表/删除)
│   └── FileAdminController.java       (5个端点: 文件分页/详情/任务分页/状态更新/删除)
├── service/
│   ├── UserFileService.java           (8个方法: 初始化/秒传/整文件/分片/完成/文件列表/任务列表/删除)
│   ├── FileAdminService.java          (5个方法: 文件分页/详情/任务分页/状态更新/删除)
│   ├── FileLifecycleService.java      (4个方法: 引用元数据刷新/引用移除同步/过期检测/批量清理)
│   ├── FileInfoService.java           (基础仓储, extends IService)
│   ├── FileUploadTaskService.java     (基础仓储, extends IService)
│   ├── FileChunkService.java          (基础仓储, extends IService)
│   └── FileBusinessInfoService.java   (基础仓储, extends IService)
├── impl/
│   ├── UserFileServiceImpl.java       (~970行, 完整上传生命周期)
│   ├── FileAdminServiceImpl.java      (~260行, 后台管理)
│   ├── FileLifecycleServiceImpl.java  (~220行, 引用计数/清理/回收)
│   └── 4个基础仓储空壳实现
├── task/
│   └── FileUploadTaskCleanupScheduler.java  (定时过期任务清理)
└── model/
    ├── user/    (8个DTO: 请求/响应/分页查询)
    └── admin/   (7个DTO: 分页查询/状态更新/VO/详情VO/引用VO/任务VO)
```

存储层（`common/storage`）:

- `StorageService` 接口 (13个方法) + `StorageManager` 路由层
- 3个实现: `LocalStorageServiceImpl` / `MinioStorageServiceImpl` / `OssStorageServiceImpl`
- 4种路由策略: DEFAULT / FAILOVER / ROUND_ROBIN / RANDOM

## 2. 功能完成度评估

### 2.1 接口方法完成情况

| 服务                   | 方法数  | 已实现  | 状态                   |
|----------------------|------|------|----------------------|
| UserFileService      | 8    | 8    | ✅ 全部完成               |
| FileAdminService     | 5    | 5    | ✅ 全部完成               |
| FileLifecycleService | 4    | 4    | ✅ 全部完成               |
| 基础仓储(4个)             | 0自定义 | 全部   | ✅ MyBatis-Plus标准CRUD |
| StorageService       | 13   | 3×13 | ✅ 3个存储后端全部实现         |

**结论: file 模块所有接口方法均已完整实现，不存在缺失的方法。**

### 2.2 上传状态机

```
INIT(0) → UPLOADING(1) → MERGING(2) → COMPLETED(3)
                                     ↘ FAILED(4) → 可重试回UPLOADING
                                     ↘ CANCELLED(5) 终态
```

完整覆盖: 秒传命中/整文件上传/分片上传+合并/失败补偿/过期取消

## 3. 本轮已完成

- [x] 实现用户侧文件上传全生命周期（初始化/秒传/整文件/分片/完成/删除）。
- [x] 实现后台文件管理（文件分页/详情/任务分页/状态更新/级联删除）。
- [x] 实现文件生命周期服务（引用计数刷新/引用移除同步/过期任务检测/批量清理）。
- [x] 实现定时过期任务清理调度器。
- [x] 实现多后端存储抽象（本地/MinIO/阿里云OSS）与路由策略。
- [x] 补入后台文件管理服务级测试（状态更新边界/删除级联清理/存储清理失败容忍）。
- [x] 补入用户文件服务级测试（初始化参数校验/秒传过期/上传失败补偿/分片失败补偿/完成失败补偿）。
- [x] 补入文件生命周期服务级测试（过期任务清理/引用元数据刷新/引用归零物理文件回收）。
- [x] 补入本地存储服务级测试（分片缺失合并失败）。
- [x] 补入文件控制器权限测试（用户上传登录要求/后台文件分页/详情/任务/状态/删除权限）。
- [x] 收口终态任务重复消费门禁、查询参数静默兜底、file_info唯一键冲突回收。
- [x] 收口分片完成阶段临时分片清理、本地分片缺失终止合并。
- [x] 收口上传任务生命周期：过期即时取消+定时批量清理。
- [x] 收口上传完成后元数据失败的对象删除补偿。
- [x] 收口上传初始化参数校验、按用户维度业务引用去重、文章附件删除联动。
- [x] 收口后台状态更新与删除的职责边界。

## 4. 现有测试文件 (8个)

| 测试文件                           | 覆盖范围                                                                                                                                                            |
|--------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `FileAdminServiceImplTest`     | 状态更新边界(拒绝DELETED/拒绝已删除记录)/删除级联清理/存储失败容忍/查询参数校验/文件分页/详情聚合/任务分页                                                                                                   |
| `UserFileServiceImplTest`      | 初始化参数校验/初始化正常返回(整文件+分片)/初始化秒传命中/非法 referenceType/category 拦截/quickCheck 命中与未命中/用户文件与任务分页/秒传过期/整文件上传成功与失败补偿/MD5 校验/终态门禁/重复键复用与已删除文件复活/分片上传成功与覆盖更新/完成上传成功与清理/删除委托 |
| `FileLifecycleServiceImplTest` | 过期任务清理/存储失败容忍/已完成任务跳过/活跃过期任务取消/批量边界循环/引用归零回收/引用残留刷新                                                                                                             |
| `LocalStorageServiceImplTest`  | 正常上传/正常合并/删除/获取 URL/临时文件清理/分片缺失合并失败                                                                                                                             |
| `StorageManagerImplTest`       | 路由策略选择/故障转移/健康检查/默认节点回退/策略切换                                                                                                                                    |
| `FileAdminControllerTest`      | 后台分页端点                                                                                                                                                          |
| `UserFileControllerTest`       | 用户初始化/文件分页端点                                                                                                                                                    |
| `FileControllerSecurityTest`   | 用户登录要求/后台权限拦截(11个权限场景)                                                                                                                                          |

## 5. 下一批高优先级

### 5.1 UserFileService 正常路径补充

- [x] `initUploadTask` - 正常初始化返回（整文件模式/分片模式）
- [x] `initUploadTask` - 秒传命中即时完成（MD5+大小匹配已有文件）
- [x] `initUploadTask` - 非法 referenceType 拦截
- [x] `initUploadTask` - 非法 category 拦截
- [x] `quickCheck` - 秒传命中返回（quickUpload=true）
- [x] `quickCheck` - 秒传未命中返回（无匹配文件）
- [x] `uploadFile` - 正常整文件上传成功（FileInfo创建+引用创建+任务完成）
- [x] `uploadFile` - MD5不匹配拦截（如启用校验）
- [x] `uploadFile` - 终态任务拒绝上传
- [x] `uploadFile` - MD5+大小去重命中（DuplicateKey冲突回收）
- [x] `uploadChunk` - 正常分片上传成功+进度刷新
- [x] `uploadChunk` - 重复分片覆盖更新
- [x] `uploadChunk` - 分片MD5不匹配拦截
- [x] `uploadChunk` - 终态任务拒绝上传
- [x] `completeUpload` - 正常合并成功+任务完成
- [x] `completeUpload` - 分片未完成拒绝合并
- [x] `completeUpload` - 合并时MD5去重命中
- [x] `completeUpload` - 合并后临时分片清理
- [x] `completeUpload` - 终态任务拒绝完成
- [x] `pageMyFiles` - 用户文件分页+过滤（关键词/状态/分类/引用类型）
- [x] `pageMyUploadTasks` - 用户任务分页+过滤

### 5.2 FileAdminService 补充

- [x] `pageFiles` - 正常分页+过滤（关键词/用户/状态/分类/可见性）
- [x] `getFile` - 详情装配（文件+引用列表+任务列表聚合）
- [x] `pageTasks` - 正常分页+过滤

### 5.3 存储层补充

- [x] `LocalStorageServiceImpl` - 正常上传/正常合并/删除/获取URL/临时文件清理
- [x] `StorageManagerImpl` - 路由策略选择/故障转移/健康检查

## 6. 中期一致性补强

- [x] 核对并发上传同一MD5的竞争场景（唯一键冲突兜底验证）
- [x] 核对已删除文件被新上传复用时的复活逻辑
- [x] 核对引用计数在高并发增减时的一致性
- [x] 核对定时清理任务的批量处理边界（每批100条+循环）
- [x] 补充高成本方法的 Javadoc 注释（如 `initUploadTask` 状态机流转、`uploadFile` 去重冲突处理）
- [x] 评估是否需要 MinIO/OSS 存储实现的专项测试（当前结论：暂不新增重度 mock 单测，后续优先考虑 Testcontainers/契约测试）

## 7. 中长期基础设施

- [x] 评估是否需要更完整的存储健康检查与自动恢复（当前结论：维持现有健康检查 + FAILOVER，自动恢复延后到真实多节点流量阶段）
- [x] 评估是否需要断点续传能力（任务重启后从已上传分片继续）（当前结论：需要，但作为后续独立能力推进）
- [x] 评估是否需要文件审核/内容安全扫描接入（当前结论：需要预留，但等待内容安全/合规需求明确后再接入）
- [x] 当文件量增长后，评估分片合并性能与大文件处理策略（当前结论：当前实现可用，后续在大文件/高并发场景再引入更流式方案）
- [x] 评估是否需要存储用量统计与配额管理（当前结论：需要，但优先级低于审核与断点续传）

## 8. 完成标志

- 上传全链路（初始化/秒传/整文件/分片/合并/完成）的正常路径和异常路径都具备服务级回归覆盖。
- 后台管理（分页/详情/状态/删除）具备基础自动化验证。
- 存储层核心操作具备基础验证。
- 当前中期一致性补强项已完成，后续 file 任务主轴转向能力演进与容量治理。
