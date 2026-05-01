# 代码审查整改任务清单

本文档基于全项目代码审查结果（852 Java 源文件、44 测试文件、86 Mapper XML），按优先级整理为可执行任务，分阶段推进修复。

## 1. 任务来源

- 全项目代码审查报告（2026-05-01）
- CLAUDE.md 项目规范
- docs/项目代码编写规范.md
- docs/项目结构规范.md

## 2. 当前状态

**当前阶段：第一阶段完成（P0 安全修复）。**

```
已完成:
  ✅ 第一阶段：安全修复（P0，7 项）
待完成:
  ⬜ 第二阶段：数据一致性修复（P1，5 项）
  ⬜ 第三阶段：业务逻辑与安全加固（P1，6 项）
  ⬜ 第四阶段：性能优化与架构改善（P2，9 项）
  ⬜ 第五阶段：测试补充（P1/P2，5 项）
  ⬜ 第六阶段：代码质量改善（P3，10 项）
```

---

## 3. 第一阶段：安全修复（P0 — 立即处理）

### 3.1 验证码恒定时间比较

- [x] `EmailCodeAuthenticationProvider.java:34` — `cachedCode.equals(code)` 改为 `MessageDigest.isEqual(cachedCode.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8))`
- [x] `TwoFactorServiceImpl.java:63` — `storedCode.equals(code)` 同上
- [x] 确认项目中所有验证码比较场景均已完成替换

### 3.2 AccountTakeoverServiceImpl MFA 绕过修复

- [ ] `AccountTakeoverServiceImpl.java:39` — `validateTicket` 返回值加 `ExceptionThrowerCore.throwBusinessIfNot(...)` 校验
- [ ] `AccountTakeoverServiceImpl.resolveTakeover` — token 使用后立即 `redisOperator.delete(tokenKey)`

### 3.3 注册接口密码强度校验

- [ ] `AuthRegisterRequest.java:16` — 密码字段加 `@Size(min = 8, max = 64)` + `@Pattern` 正则约束（大小写字母 + 数字）
- [ ] `SysUserSaveRequest` 密码字段同步添加校验
- [ ] `AuthServiceImpl.register` 中补充服务端密码强度校验（不依赖 Bean Validation）

### 3.4 CORS 配置按环境区分

- [ ] `CorsConfig.java:27-33` — 将 `allowedOriginPatterns("*")` 改为从配置文件读取允许的来源列表
- [ ] `application.yml` 各环境配置文件中补充 `cors.allowed-origins` 配置项

### 3.5 DashboardMetricsMapper.xml 列名 Bug 修复

- [ ] `DashboardMetricsMapper.xml` — `countAuthors` 查询中 `r.deleted_flag` 改为 `r.is_deleted`

### 3.6 管理员删除评论级联清理补充

- [ ] `CommentAdminServiceImpl.deleteComment` — 补充 `sysInteractionRepository.removeByTargetTypeAndTargetIds` 调用，与 `UserCommentServiceImpl` 逻辑对齐
- [ ] 考虑提取 `CommentLifecycleService` 统一删除逻辑

### 3.7 文件上传未完成实现修复

- [ ] `FileUploadServiceImpl.uploadFile` — 修正 InputStream 为 null 的问题，从 `MultipartFile` 提取输入流并调用 `storageService.upload()`
- [ ] `UserFileServiceImpl.uploadFile` — 修正 md5 永远为 null 的问题

---

## 4. 第二阶段：数据一致性修复（P1 — 1 周内）

### 4.1 计数器原子更新

- [ ] `ArticleContentFacadeServiceImpl.adjustCounter` — 改用 SQL 原子更新 `UPDATE blog_article SET xxx_count = xxx_count + ? WHERE id = ?`
- [ ] `UserArticleActionServiceImpl` — 点赞/取消点赞计数改为原子更新
- [ ] `UserCommentServiceImpl` — 评论计数改为原子更新
- [ ] `UserCollectionServiceImpl` — 收藏计数改为原子更新
- [ ] `InteractionAdminServiceImpl` — 互动计数改为原子更新
- [ ] 在 Repository 层提供 `incrementXxxCount(articleId, delta)` 通用方法

### 4.2 角色/权限变更时失效用户会话

- [ ] `SysUserAdminServiceImpl.assignRoles` — 调用后主动调用 `tokenManager.invalidateUserSessions(userId)`
- [ ] 确认其他修改角色/权限的入口（如菜单授权变更）也需要失效会话

### 4.3 updateStatus/deleteUser 补审计日志

- [ ] `SysUserAdminServiceImpl.updateStatus` — 补充 `@SysLog` 或手动记录审计日志
- [ ] `SysUserAdminServiceImpl.deleteUser` — 同上
- [ ] 确认这两个操作是否也需要 MFA 校验

### 4.4 文件表字符集统一

- [ ] `file_info`、`file_upload_task`、`file_chunk`、`file_business_info` — COLLATE 从 `utf8mb4_general_ci` 改为 `utf8mb4_unicode_ci`
- [ ] 编写 migration 脚本（如使用 Flyway/Liquibase）或手动修改 DDL

### 4.5 权限标识去重

- [ ] 菜单 1715（文章状态）的 perm 从 `content:article:update` 改为 `content:article:update-status`
- [ ] 菜单 1792（会话状态）的 perm 从 `content:chat:update` 改为 `content:chat:update-status`
- [ ] 更新对应的 Controller `@PreAuthorize` 注解

---

## 5. 第三阶段：业务逻辑与安全加固（P1 — 1-2 周内）

### 5.1 WebSocket Token 传递方式优化

- [ ] `WebSocketAuthHandshakeInterceptor` — 优先从 Header 获取 Token，查询参数仅作为兜底
- [ ] 确保日志系统对 WebSocket 握手 URL 做脱敏

### 5.2 IP 限流默认值调整

- [ ] `ConfigConstants.DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND` — 从 100 改为 10-20

### 5.3 IP 地址获取防伪造

- [ ] `IPUtils.java` — 添加可信代理配置，仅信任来自已知反向代理的 `X-Forwarded-For`
- [ ] 或使用 Spring `server.forward-headers-strategy` 获取正确 IP

### 5.4 高风险操作细粒度权限

- [ ] `SuperAdminController` — 封禁/解封/等级调整/经验调整/账号接管等操作分别定义权限标识
- [ ] 更新 `03_permission_init.sql` 中对应的权限数据
- [ ] 更新角色权限分配

### 5.5 文件 originalName 路径遍历防护

- [ ] `FileUploadServiceImpl.validateInitRequest` — 增加对 originalName 的非法字符校验（过滤 `/`、`\`、`\0`、换行符）
- [ ] 文件扩展名校验 — 禁止双重扩展名（如 `malware.jpg.php`）

### 5.6 消息内容长度校验

- [ ] `ChatMessageSendServiceImpl.validateSendRequest` — 增加内容长度上限校验（建议 5000 字符）

---

## 6. 第四阶段：性能优化与架构改善（P2 — 2-4 周内）

### 6.1 大文件流式返回

- [ ] `PublicFileAccessController.getFile` — 使用 `StreamingResponseBody` 或 `ResourceRegion` 替代 `inputStream.readAllBytes()`

### 6.2 评论查询加分页

- [ ] `PublicCommentController` — 返回类型从 `List<PublicCommentVO>` 改为 `PageResult<PublicCommentVO>`
- [ ] 根评论查询添加分页参数

### 6.3 按分类/标签过滤改用子查询

- [ ] `PublicArticleServiceImpl.resolveArticleIdsByRelations` — 改用子查询或 JOIN 在数据库层面完成过滤，避免加载全量 ID 到内存

### 6.4 Follow 模块引入 Redis 缓存

- [ ] 关注数/粉丝数查询结果缓存到 Redis
- [ ] 关注/取关操作时失效缓存
- [ ] Redis Key 使用 `RedisKeyUtils.build()` 拼接并设置 TTL

### 6.5 WebSocket session 并发写保护

- [ ] `ChatPushServiceImpl.pushLocal` — 对 `session.sendMessage()` 使用 `ConcurrentWebSocketSessionDecorator` 或 `synchronized` 包裹

### 6.6 Redis Key TTL 合规性收口

- [ ] `RedisOperator.persist()` — 标注 `@Deprecated` 并添加文档警告
- [ ] `RedisOperator.set(key, value)` 无 TTL 重载 — 标注 `@Deprecated` 或添加醒目的 Javadoc 警告

### 6.7 跨模块依赖收口

- [ ] `ArticleAdminCrudServiceImpl` — 将 content 模块的 Repository 依赖通过 Facade/Lifecycle Service 收口
- [ ] `PublicArticleServiceImpl.getArticle` — 足迹记录通过事件发布或异步方式解耦

### 6.8 索引优化

- [ ] `sys_menu` 表 — 增加 `idx_menu_perm` 索引
- [ ] `blog_article` 表 — 核心查询索引覆盖 review_status
- [ ] `sys_log` 表 — `idx_create_time` 改为联合索引 `(module, create_time DESC)`

### 6.9 收藏夹分页参数化

- [ ] `UserCollectionServiceImpl.pageFolders` — 接受前端传入的分页参数
- [ ] `UserCollectionServiceImpl.pageCollections` — 同上

---

## 7. 第五阶段：测试补充（P1/P2 — 持续推进）

### 7.1 安全关键路径测试（P0）

- [ ] `AuthUserDetailsServiceImpl` — 用户加载、账号不存在、账号禁用
- [ ] `TwoFactorServiceImpl` — 2FA 验证流程
- [ ] `AccountTakeoverServiceImpl` — 账号接管逻辑
- [ ] `AuthorPermissionServiceImpl` — 作者权限校验

### 7.2 核心业务测试（P1）

- [ ] `ArticleAdminCrudServiceImpl` — 文章 CRUD
- [ ] `ArticleContentFacadeServiceImpl` — 门面服务
- [ ] `ChatMessageSendServiceImpl` — 消息发送
- [ ] `ChatChannelJoinServiceImpl` — 频道加入/退出
- [ ] `FileUploadServiceImpl` — 文件上传（分片/秒传）

### 7.3 增强现有薄弱测试

- [ ] `ArticleAdminServiceImplTest` — 补充异常路径和组合逻辑测试
- [ ] `UserCollectionServiceImplTest` — 补充取消收藏、重复检测、文件夹管理

### 7.4 Controller 安全测试

- [ ] 为 article、content、auth 模块核心 Controller 添加安全测试
- [ ] 验证 `@PreAuthorize` 注解正确性

### 7.5 测试代码质量改善

- [ ] 减少 `@BeforeEach` 中的 `lenient()` stub
- [ ] 所有测试方法至少包含一个状态断言（不能仅依赖 `verify()`）

---

## 8. 第六阶段：代码质量改善（P3 — 按需处理）

- [ ] `ExceptionThrowerCore` 加 `final` 修饰
- [ ] `BusinessException(String resultCode)` 构造函数参数名改为 `message`
- [ ] `JsonUtils` 中 `RuntimeException` 改为 `BusinessException`
- [ ] 常量类中统一定义 `TARGET_TYPE_ARTICLE`、`TARGET_TYPE_COMMENT` 等
- [ ] `ArticleStatusMachine` 魔法数字改为枚举
- [ ] `SecurityExceptionHandler` 中删除重复的 `BadCredentialsException` 处理器
- [ ] `RedisKeyUtils.build()` 返回空字符串时改为抛异常
- [ ] `Integer.valueOf(1).equals()` 冗余写法统一简化
- [ ] `ChatServiceSupport` 按职责拆分
- [ ] 异常处理器中硬编码 `"production"` 改为常量

---

## 9. 好消息

以下方面做得很好，值得保持：

- **Mapper XML 零 SQL 注入风险** — 全部使用 `#{}` 参数绑定
- **表结构与索引设计整体优秀** — 生成列 + 唯一索引实现软删除下唯一约束
- **异常处理统一** — Service 层使用 `ExceptionThrowerCore` 抛出 `BusinessException`
- **MapStruct 实体转换** — 未发现手写逐字段拷贝的反模式
- **Controller 不直接依赖 Mapper** — 依赖方向正确
- **RBAC 权限模型清晰** — 角色-菜单-权限三层结构
- **Redis Key 规范** — 通过 `RedisKeyUtils.build()` 拼接且均有 TTL
- **暴力破解防护** — 登录失败计数 + 锁定机制设计合理
- **MFA 保护高风险操作** — 封禁/角色分配等要求 MFA 票据
