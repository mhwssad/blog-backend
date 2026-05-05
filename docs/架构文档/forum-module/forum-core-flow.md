# Forum 核心流程

## 1. 发帖流程

### 1.1 用户发帖

```
用户发帖 → Controller
    │
    ▼
UserForumService.createPost()
    │
    ├─ 1. 校验版块可用性（requirePostableSection）
    │     - 版块存在且启用
    │     - 版块登录可见时用户已登录
    │     - 用户等级 >= 版块发帖等级限制
    │
    ├─ 2. 构建 ForumPost 实体
    │     - sectionId, authorId, title, content
    │     - status: 根据请求参数归一化（草稿/发布）
    │     - visibilityScope: 根据请求参数归一化
    │     - isTop=0, isEssence=0, 计数初始化为0
    │     - 若为发布状态，设置 publishedAt
    │
    ├─ 3. 保存帖子 forumPostRepository.save()
    │
    ├─ 4. 经验值 +UserExperience（发帖）
    │
    └─ 5. 若是发布状态，发布 ContentChangeEvent（AI 知识同步）
```

**关键代码路径**：`UserForumServiceImpl.createPost()` [L82-107]

### 1.2 版块发帖等级校验

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/forum/service/impl/UserForumServiceImpl.java#L332-340
private ForumSection requirePostableSection(Long sectionId, Long userId) {
    // 1. 版块存在且启用
    // 2. 登录可见版块需登录
    // 3. 用户等级 >= 版块发帖等级限制
}
```

## 2. 回复流程

### 2.1 用户回复

```
用户回复 → Controller
    │
    ▼
UserForumService.createReply()
    │
    ├─ 1. 校验帖子可回复（requireReplyablePost）
    │     - 帖子存在
    │     - 帖子状态为已发布
    │
    ├─ 2. 构建 ForumReply 实体
    │     - postId, userId, content
    │     - parentId, rootId（树形结构）
    │     - floorNo = nextFloorNo(postId)
    │     - status = 正常
    │
    ├─ 3. 若有父回复
    │     - 校验父回复属于同一帖子
    │     - 设置 rootId
    │     - 父回复 replyCount++
    │
    ├─ 4. 保存回复
    │
    ├─ 5. 帖子回复数 ++
    │
    ├─ 6. 通知帖子作者（非自己）
    │
    ├─ 7. 若有父回复，通知父回复作者（非自己、非帖子作者）
    │
    └─ 8. 经验值 +UserExperience（回复）
```

**关键代码路径**：`UserForumServiceImpl.createReply()` [L151-203]

### 2.2 回复树形结构

```
根回复（parentId=0, rootId=0）
├── 子回复A（parentId=根ID, rootId=根ID）
│   └── 子回复A1（parentId=A的ID, rootId=根ID）
└── 子回复B（parentId=根ID, rootId=根ID）
```

- `parentId=0` 表示根回复
- `rootId=0` 表示自身为根
- 子回复的 `rootId` 指向根回复ID

## 3. 互动流程

### 3.1 点赞

```
UserForumService.likePost(postId)
    │
    ├─ 1. 校验帖子归属和状态（requireInteractablePost）
    │
    ├─ 2. 检查是否已点赞
    │
    ├─ 3. 点赞记录 SysInteraction（targetType=forum_post, actionType=like）
    │
    └─ 4. ForumPost.likeCount++
```

### 3.2 收藏

```
UserForumService.collectPost(postId, request)
    │
    ├─ 1. 获取或创建默认论坛收藏夹（getOrCreateDefaultForumFolder）
    │
    ├─ 2. 检查是否已收藏
    │
    ├─ 3. 收藏记录 SysCollection
    │
    └─ 4. ForumPost.collectCount++
```

### 3.3 分享到频道

```
UserForumService.sharePostToChannel(postId, conversationId)
    │
    ├─ 1. 校验帖子归属
    │
    ├─ 2. 创建 ForumPostChannelLink
    │     - forumPostId, conversationId
    │     - linkType = "manual_share"
    │     - linkedBy = 当前用户
    │
    └─ 3. ForumPost.shareCount++
```

## 4. 后台治理流程

### 4.1 隐藏帖子

```
ForumPostAdminService.hidePost(id, ...)
    │
    ├─ 1. 获取帖子，校验状态（非已删除）
    │
    ├─ 2. forumPostRepository.updateStatusById(HIDDEN)
    │
    ├─ 3. 关联频道链接失效
    │
    ├─ 4. 记录审计日志 SysAuditLog
    │
    └─ 5. 发布 ContentChangeEvent（HIDE）
```

### 4.2 删除帖子

```
ForumPostAdminService.deletePost(id, ...)
    │
    ├─ 1. 获取帖子，校验状态（非已删除）
    │
    ├─ 2. forumPostRepository.updateStatusById(DELETED)
    │
    ├─ 3. 关联频道链接失效
    │
    ├─ 4. 记录审计日志
    │
    └─ 5. 发布 ContentChangeEvent（DELETE）
```

### 4.3 设为精华

```
ForumPostAdminService.toggleEssence(id, enabled, ...)
    │
    ├─ 1. 更新 isEssence
    │
    ├─ 2. 记录审计日志
    │
    └─ 3. 若设为精华，通知作者（NotificationDeliveryService）
```

## 5. 公开浏览流程

### 5.1 查询帖子详情

```
PublicForumService.getPost(id)
    │
    ├─ 1. 获取帖子，校验状态（已发布）
    │
    ├─ 2. 校验可见性（登录可见需登录）
    │
    ├─ 3. 校验版块可用性和可见性
    │
    ├─ 4. 补充版块名称、作者名称
    │
    ├─ 5. 检查当前用户是否点赞/收藏
    │
    ├─ 6. 加载关联的频道信息
    │
    └─ 7. 浏览数 ++（异步/同步）
```

**关键代码路径**：`PublicForumServiceImpl.getPost()` [L91-102]

### 5.2 查询回复列表

```
PublicForumService.pageReplies(postId, current, size)
    │
    ├─ 1. 校验帖子可访问（同 getPost）
    │
    ├─ 2. 查询所有正常回复（listByPostId）
    │
    ├─ 3. 分页查询根回复（pageRootReplies）
    │
    ├─ 4. 递归构建回复树（buildReplyTree）
    │
    └─ 5. 补充作者名称
```

## 6. AI 知识同步

### 6.1 事件发布

```
ContentChangeEvent
    - sourceType: "FORUM_POST"
    - targetId: 帖子ID
    - action: HIDE / RESTORE / DELETE
    - operatorId: 操作人ID
```

### 6.2 事件监听

`KnowledgeSyncContentListener` 监听内容变更事件，触发知识库同步。

## 7. 状态流转图

### 7.1 帖子状态流转

```
     ┌─────────────────────────────────────────┐
     │                                         │
     ▼                                         │
  草稿(0) ──发布──▶ 已发布(1) ──后台隐藏──▶ 隐藏(5)
     │               │                    ▲
     │               │                    │
     │               ▼                    │
     │           审核中(2)                │
     │               │                    │
     │               ▼                    │
     │           已拒绝(3) ──重新编辑──▶  │
     │                                   │
     └───────用户删除──▶ 已删除(4) ───────┘
```

### 7.2 回复状态流转

```
  正常(1) ──后台隐藏──▶ 隐藏(2)
    │
    │
    ▼
 用户删除 ──▶ 已删除(3)
    │
    │
    ▼
  审核中(4)
```