# 评论模块流程

## 1. 概述

评论模块（`comment` 子域）负责文章评论、点赞、回复树结构与评论审核管理。

## 2. 核心实体

### SysComment（评论）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| targetId | Long | 评论目标ID |
| targetType | String | 目标类型（article） |
| userId | Long | 评论者ID |
| content | String | 评论内容 |
| images | String | 评论图片（JSON数组） |
| rootId | Long | 根评论ID（顶级为0） |
| parentId | Long | 父评论ID（顶级为0） |
| likeCount | Integer | 点赞数 |
| replyCount | Integer | 回复数 |
| status | Integer | 状态（0=待审核，1=通过，2=拒绝） |

### SysInteraction（互动记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 操作用户 |
| targetId | Long | 互动目标ID |
| targetType | String | 目标类型（comment） |
| actionType | String | 互动类型（like） |

## 3. 服务接口

### UserCommentService（用户侧）

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/content/comment/service/UserCommentService.java#L1-17
public interface UserCommentService {
    void likeComment(Long commentId);          // 点赞评论
    void unlikeComment(Long commentId);       // 取消点赞
    void createComment(CommentSaveRequest request);  // 发表评论
    void deleteComment(Long commentId);        // 删除评论
}
```

### CommentAdminService（后台管理）

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/content/comment/service/CommentAdminService.java
public interface CommentAdminService {
    PageResult<CommentVO> pageComments(CommentPageQuery query);  // 分页查询
    CommentVO getComment(Long id);                               // 获取详情
    void updateStatus(Long id, Integer status);                 // 审核状态
    void deleteComment(Long id);                                 // 删除评论
}
```

## 4. 核心流程

### 4.1 发表评论

```
用户发表评论
        │
        ▼
校验目标类型（仅支持 article）
        │
        ▼
获取文章（校验可互动状态）
        │
        ▼
构建评论实体
   ├── targetType = "article"
   ├── userId = 当前用户
   ├── rootId = parentId==null ? 0 : (parent.rootId==0 ? parent.id : parent.rootId)
   ├── parentId = parentId==null ? 0 : parentId
   ├── likeCount = 0
   ├── replyCount = 0
   └── status = 1（直接通过）
        │
        ▼
如果是回复 → 更新父评论回复数（+1）
        │
        ▼
保存评论
        │
        ▼
更新文章评论计数（+1）
        │
        ▼
发放经验值（评论创建奖励）
        │
        ▼
发送通知（作者被评论） ── 仅当文章作者≠评论者
```

### 4.2 删除评论（树形）

```
用户删除评论
        │
        ▼
校验评论归属（只能删除自己的评论）
        │
        ▼
获取目标下全部评论
        │
        ▼
BFS遍历构建评论子树
   找出该评论及其所有子孙回复
        │
        ▼
批量删除子树评论
        │
        ▼
批量删除子树交互记录（点赞）
        │
        ▼
更新文章评论计数（-N，N=子树大小）
        │
        ▼
如果是回复 → 更新父评论回复数（-1）
```

### 4.3 点赞评论

```
用户点赞评论
        │
        ▼
检查是否已点赞（userId + targetId + targetType + actionType）
        │已点赞 → 直接返回（幂等）
        ▼未点赞
        ▼
保存互动记录（actionType="like"）
        │
        ▼
更新评论点赞数（+1）
```

### 4.4 取消点赞

```
用户取消点赞
        │
        ▼
查询点赞记录
        │不存在 → 直接返回
        ▼存在
        ▼
删除互动记录
        │
        ▼
更新评论点赞数（-1）
```

## 5. 评论树结构

评论采用**根评论 + 回复**的扁平树结构：

```
rootId = 0, parentId = 0  →  顶级评论（根评论）
rootId = N, parentId = M  →  回复（N的子节点）
```

**查询流程**：
1. 分页查询目标下根评论（status=1）
2. 批量查询根评论的所有回复
3. 前端自行组装树形结构

## 6. 计数联动

评论模块与文章模块存在计数联动：

| 操作 | 评论 replyCount | 文章 commentCount |
|------|-----------------|-------------------|
| 发表根评论 | - | +1 |
| 发表回复 | parent +1 | +1 |
| 删除评论树 | - | -N |
| 删除回复 | parent -1 | -N |
| 点赞 | +1 | - |
| 取消点赞 | -1 | - |

## 7. API 路由

| 路由 | 方法 | 说明 | 鉴权 |
|------|------|------|------|
| `/api/user/comments/{id}/likes` | POST | 点赞评论 | 用户 |
| `/api/user/comments/{id}/likes` | DELETE | 取消点赞 | 用户 |
| `/api/user/comments` | POST | 发表评论 | 用户 |
| `/api/user/comments/{id}` | DELETE | 删除评论 | 用户 |
| `/api/public/comments` | GET | 查询目标评论 | 公开 |
| `/api/sys/comments` | GET | 后台分页查询 | 管理员 |
| `/api/sys/comments/{id}` | GET | 评论详情 | 管理员 |
| `/api/sys/comments/{id}/status` | PUT | 审核评论 | 管理员 |
| `/api/sys/comments/{id}` | DELETE | 删除评论 | 管理员 |

## 8. 相关文档

- [Content 模块总览](./00-content-module-overview.md)