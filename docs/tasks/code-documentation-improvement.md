# 代码注释改善任务

## 概述

全面改善项目各层的 Javadoc 和行注释，提高代码可读性。

## 注释规范

- **语言**：中文（与现有代码一致）
- **类注释**：`/** 一句话摘要。<p>详细描述职责。 */`
- **方法注释**：`/** 一句话摘要。 */`，复杂方法加 `@param`/`@return`
- **行注释**：仅解释 WHY（业务约束、设计决策），不解释 WHAT
- **不添加**：显而易见的注释、重复 @Schema 的 Javadoc、多余分隔线

## 执行进度

### P0: Domain 实体字段注释 (31 文件)

> 参照 `SysUser.java` 的字段注释风格，为所有实体补全字段 Javadoc。

**auth 模块 (9)**

- [x] SysRole.java
- [x] SysMenu.java
- [x] SysConfig.java
- [x] SysLog.java
- [x] SysNotice.java
- [x] SysUserNotice.java
- [x] SysRoleMenu.java
- [x] SysUserRole.java
- [x] SysUser.java（已有完整字段注释，作为参照）

**content 模块 (8)**

- [x] SysCategory.java
- [x] SysTag.java
- [x] SysTagRelation.java
- [x] SysComment.java
- [x] SysInteraction.java
- [x] SysCollection.java
- [x] SysCollectionFolder.java
- [x] SysUserFootprint.java

**article 模块 (3)**

- [x] BlogArticle.java
- [x] BlogArticleCategory.java
- [x] BlogArticleAccess.java

**chat 模块 (6)**

- [x] ChatConversation.java
- [x] ChatConversationMember.java
- [x] ChatMessage.java
- [x] ChatMessageRecipient.java
- [x] ChatMessageReadCursor.java
- [x] ChatAttachmentProcessTask.java

**file 模块 (4)**

- [x] FileInfo.java
- [x] FileUploadTask.java
- [x] FileChunk.java
- [x] FileBusinessInfo.java

**follow 模块 (1)**

- [x] SysUserFollow.java

### P1: Service impl 方法注释 (39 文件)

> 为所有公开方法补全 Javadoc，为复杂私有方法加注释。

**auth 模块 (10)**

- [x] AuthServiceImpl.java（当前零方法注释，优先处理）
- [x] AuthUserDetailsServiceImpl.java
- [x] SysConfigAdminServiceImpl.java
- [x] SysConfigServiceImpl.java
- [x] SysLogAdminServiceImpl.java
- [x] SysMenuAdminServiceImpl.java
- [x] SysNoticeAdminServiceImpl.java
- [x] SysRoleAdminServiceImpl.java
- [x] SysUserAdminServiceImpl.java
- [x] UserNoticeInboxServiceImpl.java

**article 模块 (4)**

- [x] ArticleAccessControlServiceImpl.java
- [x] ArticleAdminServiceImpl.java
- [x] PublicArticleServiceImpl.java
- [x] UserArticleActionServiceImpl.java

**chat 模块 (9)**

- [x] ChatAdminServiceImpl.java
- [x] ChatAttachmentAsyncProcessingServiceImpl.java
- [x] ChatAttachmentMetadataResolverImpl.java
- [x] ChatMessageGovernanceServiceImpl.java
- [x] ChatMetricsServiceImpl.java
- [x] ChatPushRedisSubscriber.java
- [x] ChatPushServiceImpl.java
- [x] ChatWebSocketSessionRegistryImpl.java
- [x] UserChatServiceImpl.java

**content 模块 (10)**

- [x] CategoryAdminServiceImpl.java
- [x] CollectionAdminServiceImpl.java
- [x] CommentAdminServiceImpl.java
- [x] FootprintAdminServiceImpl.java
- [x] InteractionAdminServiceImpl.java
- [x] PublicContentQueryServiceImpl.java
- [x] TagAdminServiceImpl.java
- [x] UserCollectionServiceImpl.java
- [x] UserCommentServiceImpl.java
- [x] UserFootprintServiceImpl.java

**file 模块 (3)**

- [x] FileAdminServiceImpl.java
- [x] FileLifecycleServiceImpl.java
- [x] UserFileServiceImpl.java

**follow 模块 (4)**

- [x] FollowAdminServiceImpl.java
- [x] FollowNoticeServiceImpl.java
- [x] PublicFollowServiceImpl.java
- [x] UserFollowServiceImpl.java

### P2: Repository 方法注释 (31 对接口+实现)

> 为所有 Repository 接口方法声明和实现类方法补全 Javadoc，复杂查询构建逻辑加行注释。

全部 31 对 Repository 接口+实现已完成。

### P3: Utility/Common 方法注释

> 为工具类公开方法补全 Javadoc。

已处理：

- common/redis/ — RedisOperator、RedisKeyUtils
- common/storage/ — StorageService 接口及所有实现、工厂类、健康检查
- common/constant/ — 7 个常量类
- common/annotation/ — DisableSysLog
- common/aspect/ — SysLogAspect（已有完整注释）
- module/**/convert/ — 9 个 MapStruct 转换器

### P4: Controller 类注释 (30 文件)

> 改善类级 Javadoc，方法已有 Swagger 注释无需重复。

全部 30 个 Controller 已完成。

### P5: Config/Exception 注释 (35 文件)

> 为配置类和异常处理器补全注释。

已处理：

- config/ — 24 个配置类（含 property 和 websocket 子包）
- module/chat/config/ — ChatRedisPushConfig
- exception/ — 10 个异常类和处理器
