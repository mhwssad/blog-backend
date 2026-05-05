# Auth 通知体系

## 1. 概述

通知体系负责用户通知的发送与管理，包括：

- **系统通知**：平台向全体或指定用户发送通知
- **用户通知**：针对单个用户的通知（评论、点赞、关注等）
- **通知偏好**：用户可设置各类通知的接收开关

## 2. 核心数据模型

### 2.1 通知表结构

| 表名 | 说明 |
|------|------|
| sys_notice | 系统通知模板 |
| sys_user_notice | 用户通知记录 |
| sys_user_notification_setting | 用户通知偏好设置 |

### 2.2 通知类型 (NotificationTypeEnum)

| Code | 标签 | 说明 |
|------|------|------|
| comment_me | 评论我 | 评论通知 |
| like_me | 点赞我 | 点赞通知 |
| collect_article | 收藏我文章 | 收藏通知 |
| follow_me | 有人关注我 | 关注通知 |
| private_message | 收到私聊 | 私聊通知 |
| group_mention | 群聊有人@我 | 群聊@通知 |
| channel_announcement | 频道公告 | 频道公告 |
| system_announcement | 系统公告 | 系统公告 |
| ai_task_done | AI任务完成 | AI任务通知 |
| report_result | 举报处理结果 | 举报结果通知 |
| forum_post_essence | 论坛帖子设为精华 | 精华通知 |
| forum_reply_me | 论坛回复我 | 论坛回复通知 |
| forum_like_me | 论坛点赞我 | 论坛点赞通知 |

## 3. 通知类型说明

### 3.1 系统公告类

- `system_announcement`：平台重要公告
- `channel_announcement`：频道公告

### 3.2 互动通知类

- `comment_me`：有人评论了我的内容
- `forum_reply_me`：论坛有人回复我
- `like_me`：有人点赞了我的内容
- `forum_like_me`：论坛有人点赞我
- `collect_article`：有人收藏了我的文章

### 3.3 社交通知类

- `follow_me`：有人关注了我
- `private_message`：收到私聊消息
- `group_mention`：群聊中被@

### 3.4 功能通知类

- `ai_task_done`：AI 任务完成
- `report_result`：举报处理结果
- `forum_post_essence`：帖子被设为精华

## 4. 通知投递流程

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│  业务触发    │ ───▶ │ Notification │ ───▶ │  投递服务    │
│             │      │   Delivery    │      │  (多渠道)   │
└─────────────┘      │   Service     │      └─────────────┘
                     └──────────────┘            │
                           │                    │
                           ▼                    ▼
                    ┌──────────────┐      ┌─────────────┐
                    │ 偏好检查     │      │  WebSocket  │
                    │ (开关校验)   │      │   推送       │
                    └──────────────┘      └─────────────┘
                           │                    │
                           ▼                    ▼
                    ┌──────────────┐      ┌─────────────┐
                    │ 用户通知表   │      │  实时通知    │
                    │ 写入        │      │   展示       │
                    └──────────────┘      └─────────────┘
```

## 5. 通知偏好设置

### 5.1 偏好设置结构

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| commentNoticeEnabled | Integer | 1 | 评论通知 |
| likeNoticeEnabled | Integer | 1 | 点赞通知 |
| collectNoticeEnabled | Integer | 1 | 收藏通知 |
| followNoticeEnabled | Integer | 1 | 关注通知 |
| privateChatNoticeEnabled | Integer | 1 | 私聊通知 |
| mentionNoticeEnabled | Integer | 1 | @通知 |
| channelAnnouncementEnabled | Integer | 1 | 频道公告 |
| systemNoticeEnabled | Integer | 1 | 系统公告 |
| aiTaskNoticeEnabled | Integer | 1 | AI任务通知 |
| reportResultNoticeEnabled | Integer | 1 | 举报结果通知 |
| forumEssenceNoticeEnabled | Integer | 1 | 精华通知 |
| forumReplyNoticeEnabled | Integer | 1 | 论坛回复通知 |
| forumLikeNoticeEnabled | Integer | 1 | 论坛点赞通知 |

### 5.1 偏好检查逻辑

```java
public boolean shouldNotify(NotificationTypeEnum type, SysUserNotificationSetting setting) {
    if (setting == null) {
        // 未设置偏好，默认全部接收
        return true;
    }
    return type.isEnabled(setting);
}
```

## 6. 投递服务 (NotificationDeliveryService)

### 6.1 核心职责

1. **偏好检查**：根据用户通知偏好决定是否投递
2. **多渠道发送**：支持 WebSocket 实时推送、站内通知
3. **去重处理**：同一业务触发避免重复通知
4. **异步处理**：通知投递异步执行，不影响主流程

### 6.2 服务接口

```java
public interface NotificationDeliveryService {
    /**
     * 投递通知
     */
    void deliver(Long userId, NotificationTypeEnum type, String title, String content,
                String sourceBizId, Map<String, Object> extraData);

    /**
     * 批量投递
     */
    void deliverBatch(List<Long> userIds, NotificationTypeEnum type, String title,
                      String content, String sourceBizId);
}
```

## 7. 接口列表

### 7.1 用户侧接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/notices/page` | GET | 分页查询我的通知 |
| `/api/user/notices/unread-count` | GET | 获取未读通知数量 |
| `/api/user/notices/read-all` | PUT | 全部标为已读 |
| `/api/user/notices/read/{id}` | PUT | 标记通知为已读 |
| `/api/user/notification-settings` | GET | 获取通知偏好设置 |
| `/api/user/notification-settings` | PUT | 更新通知偏好设置 |
| `/api/user/notification-settings/batch` | PUT | 批量更新通知偏好 |

### 7.2 管理端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/notices/page` | GET | 分页查询系统通知 |
| `/api/admin/notices/{id}` | GET | 获取通知详情 |
| `/api/admin/notices` | POST | 创建系统通知 |
| `/api/admin/notices/{id}` | PUT | 更新系统通知 |
| `/api/admin/notices/{id}/publish` | PUT | 发布通知 |
| `/api/admin/notices/{id}/revoke` | PUT | 撤回通知 |
| `/api/admin/notices/{id}` | DELETE | 删除通知 |
| `/api/admin/notices/user-page` | GET | 查询用户通知（管理端） |

### 7.3 通知查询字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 通知ID |
| type | String | 通知类型 |
| title | String | 通知标题 |
| content | String | 通知内容 |
| sourceBizId | String | 来源业务ID |
| isRead | Boolean | 是否已读 |
| readAt | LocalDateTime | 阅读时间 |
| createdAt | LocalDateTime | 创建时间 |

## 8. 服务层组件

### 8.1 SysNoticeAdminService

| 方法 | 说明 |
|------|------|
| pageNotices(query) | 分页查询系统通知 |
| getNotice(id) | 获取通知详情 |
| createNotice(request) | 创建通知 |
| updateNotice(id, request) | 更新通知 |
| publishNotice(id) | 发布通知 |
| revokeNotice(id) | 撤回通知 |
| deleteNotice(id) | 删除通知 |

### 8.2 UserNoticeInboxService

| 方法 | 说明 |
|------|------|
| pageMyNotices(query) | 分页查询我的通知 |
| getUnreadCount(userId) | 获取未读数量 |
| markAsRead(id) | 标记已读 |
| markAllAsRead(userId) | 全部标为已读 |

### 8.3 UserNotificationSettingService

| 方法 | 说明 |
|------|------|
| getSetting(userId) | 获取通知偏好 |
| updateSetting(userId, request) | 更新偏好 |
| batchUpdateSetting(userId, request) | 批量更新偏好 |

## 9. 关键设计

### 9.1 通知工厂 (SysNoticeFactory)

不同类型的通知通过工厂创建，保持通知创建逻辑统一：

```java
public interface NoticeCreator {
    String getType();
    void createNotice(SysNotice notice, Map<String, Object> params);
}
```

### 9.2 投递幂等性

```java
// Redis Key 设计
Key: notice:idempotent:{bizId}:{userId}:{type}
TTL: 24小时

if (redis.setIfAbsent(key, "1", 24h)) {
    // 首次投递，执行通知发送
} else {
    // 重复请求，跳过
}
```

### 9.3 实时通知

WebSocket 推送用于实时通知展示：

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│  投递服务    │ ───▶ │  WebSocket   │ ───▶ │  前端订阅    │
│             │      │   Handler    │      │             │
└─────────────┘      └──────────────┘      └─────────────┘
```

### 9.4 未读计数

用户未读通知数量缓存于 Redis：

```
Key: user:notice:unread:{userId}
Value: 未读数量
更新策略：
  - 投递通知时 +1
  - 标记已读时 -1
  - 标记全部已读时 清零
```
