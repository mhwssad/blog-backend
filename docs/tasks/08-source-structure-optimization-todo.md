# 源码结构优化任务清单

本文档用于把 [源码结构优化方案](../源码结构优化方案.md) 转换成可执行任务，按“先修边界、再拆重型类、再做子域化、最后做顶层提纯”的顺序推进，避免一次性大搬迁影响当前业务开发。

## 1. 任务来源

- `docs/源码结构优化方案.md`
- `docs/项目代码编写规范.md`
- `docs/项目结构规范.md`

## 2. 当前状态

**当前阶段：第二阶段进行中。重型Service拆分全部完成。**

```
已完成:
  ✅ 第一阶段全部边界收口 (Controller→Service, Service→Repository, 跨模块Repository)
  ✅ Step 1: 更新07复选框
  ✅ Step 2: 提取Chat共享Helper (ChatPayloadHelper/ChatMemberHelper/ChatPushPayloadBuilder)
  ✅ Step 3: 拆分UserChatServiceImpl → 5子Service + 1SupportHelper + 门面
  ✅ Step 4: 拆分ChatAdminServiceImpl → 2子Service + 门面
  ✅ Step 5: 拆分UserFileServiceImpl → FileUploadService + UserFileQueryService + 门面
  ✅ Step 6: 拆分ArticleAdminServiceImpl → ArticleAdminCrudService + ArticleAdminModerationService + 门面
  ✅ 拆分ChatAttachmentAsyncProcessingServiceImpl → ImageProcessor + VoiceProcessor + 门面
  ✅ 拆分ArticleSeriesServiceImpl → ArticleSeriesItemService + 门面
  ✅ ai/report 预备模块状态定义

待推进:
  ⏳ auth/chat/content 子域化
  ⏳ domain/mapper/resources 分组优化
  ⏳ utils 结构提纯
```

## 3. 第一阶段：先修边界，不搬目录

### 3.1 Controller 绕过 Service 收口

- [x] `UserExperienceController` 改为只依赖 `UserExperienceService`
- [x] 当前等级展示信息收口到 service 查询入口

### 3.2 Service 持久化泄漏收口

- [x] `SysConfigService` 移除 `IService` 继承
- [x] `SysConfigServiceImpl` 移除 `ServiceImpl` / `baseMapper`
- [x] 配置更新改为显式业务入口 `updateConfig`

### 3.3 跨模块 Repository 直连收口

#### 3.3.1 `auth -> article`

- [x] 新增 `article` 对外作者主页统计查询入口
- [x] `PublicAuthorProfileServiceImpl` 改为依赖 `article` facade/query service
- [x] `auth` 不再直接依赖 `BlogArticleRepository` / `BlogArticleSeriesRepository`

#### 3.3.2 `content -> article`

- [x] 新增 `article` 对外内容互动/访问 facade
- [x] `UserCommentServiceImpl` 改为依赖 `article` facade
- [x] `UserCollectionServiceImpl` 改为依赖 `article` facade
- [x] `PublicContentQueryServiceImpl` 改为依赖 `article` facade
- [x] `UserFootprintServiceImpl` 改为依赖 `article` facade
- [x] `InteractionAdminServiceImpl` / `CommentAdminServiceImpl` / `CollectionAdminServiceImpl` 改为依赖 `article` facade

#### 3.3.3 `chat -> file`

- [x] 新增 `file` 对外附件引用 / 文件查询 facade
- [x] `UserChatServiceImpl` 改为依赖 `file` facade
- [x] `ChatAdminServiceImpl` 改为依赖 `file` facade
- [x] `ChatAttachmentAsyncProcessingServiceImpl` 改为依赖 `file` facade

## 4. 第二阶段：拆重型类

### 4.1 chat

- [x] 拆分 `UserChatServiceImpl`
- [x] 拆分 `ChatAdminServiceImpl`
- [ ] 拆分 `ChatAttachmentAsyncProcessingServiceImpl`

### 4.2 file

- [x] 拆分 `UserFileServiceImpl`

### 4.3 article

- [x] 拆分 `ArticleAdminServiceImpl`
- [ ] 拆分 `ArticleSeriesServiceImpl`

## 5. 第三阶段：模块内部子域化

### 5.1 auth

- [ ] 明确 `account / author / rbac / notice / experience / config / audit` 子域结构
- [ ] 新增代码默认落入对应子域
- [ ] 历史高频改动类逐步迁移

### 5.2 chat

- [ ] 明确 `conversation / member / message / attachment / governance / push / websocket / shared` 子域结构
- [ ] 新增代码默认落入对应子域

### 5.3 content

- [ ] 明确 `comment / collection / taxonomy / interaction / footprint / shared` 子域结构
- [ ] 新增代码默认落入对应子域

## 6. 第四阶段：顶层提纯

### 6.1 domain / mapper / resources 分组

- [ ] `domain` 按业务域分包
- [ ] `mapper` 按业务域分包
- [ ] `resources/.../mapper` 按业务域分目录

### 6.2 utils 结构提纯

- [ ] 识别应迁往 `core` / `common` / `module/file/support` 的工具类
- [ ] 限制新增“伪通用”工具进入 `utils`

### 6.3 预备模块定义

- [x] 为 `ai` / `report` 增加”预备模块”状态说明
- [x] 在结构规范中补充 pre-module / schema-aligned module 规则

## 7. 完成标志

- Controller 不直接依赖 Repository / Mapper。
- 核心业务 Service 不再承担 Repository 角色。
- 跨模块访问优先通过 facade / query service 收口。
- chat / file / article 等重型类拆分为职责更清晰的协作类。
- `auth / chat / content` 完成子域化收口。
- `domain / mapper / resources` 与 `utils` 结构增长可控。
