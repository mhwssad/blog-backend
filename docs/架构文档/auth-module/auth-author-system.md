# Auth 作者申请体系

## 1. 概述

作者申请体系负责用户作者资质的申请与审核管理：

- **作者申请**：用户提交作者资质申请
- **资料审核**：管理员审核用户提交的资料
- **状态流转**：申请状态的完整生命周期管理
- **权限授予**：审核通过后授予作者权限

## 2. 核心数据模型

### 2.1 作者申请表 (sys_author_application)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 申请用户ID |
| applyStatus | Integer | 申请状态 |
| applyReason | String | 申请说明 |
| contentDirection | String | 擅长内容方向 |
| introduction | String | 个人简介 |
| sampleLinksJson | String | 示例链接JSON数组 |
| reviewerId | Long | 审核人ID |
| reviewComment | String | 审核备注 |
| submittedAt | LocalDateTime | 提交时间 |
| reviewedAt | LocalDateTime | 审核时间 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

## 3. 申请状态

### 3.1 状态枚举 (AuthorApplicationStatusEnum)

| 值 | 标签 | 说明 |
|----|------|------|
| 0 | PENDING | 待审核 |
| 1 | APPROVED | 已通过 |
| 2 | REJECTED | 已拒绝 |
| 3 | NEED_MORE_INFO | 待补充 |

### 3.2 状态流转图

```
                                    ┌─────────────────┐
                                    │                 │
                                    │    PENDING      │
                                    │    (待审核)      │
                                    │                 │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
                    ▼                        ▼                        ▼
          ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
          │                 │      │                 │      │                 │
          │    APPROVED     │      │    REJECTED     │      │  NEED_MORE_INFO │
          │    (已通过)      │      │    (已拒绝)      │      │    (待补充)      │
          │                 │      │                 │      │                 │
          └─────────────────┘      └─────────────────┘      └────────┬────────┘
                                                                     │
                                                                     ▼
                                                            ┌─────────────────┐
                                                            │                 │
                                                            │    PENDING      │
                                                            │    (重新待审核)  │
                                                            │                 │
                                                            └─────────────────┘
```

## 4. 申请流程

### 4.1 用户提交申请

```
POST /api/user/author/application
Body: {
    applyReason: "申请说明",
    contentDirection: "擅长内容方向",
    introduction: "个人简介",
    sampleLinks: ["https://..."]
}
```

**处理流程**：

```
1. 校验用户是否已有待审核或已通过的申请
2. 创建申请记录，状态 = PENDING
3. 设置提交时间 submittedAt = now()
4. 返回申请记录
```

### 4.2 管理员审核

```
POST /api/admin/author/applications/{id}/review
Body: {
    decision: "approve" | "reject" | "needMoreInfo",
    comment: "审核备注"
}
```

**审核流程**：

```
1. 校验申请状态为 PENDING
2. 更新审核信息：
   - reviewerId = 当前管理员ID
   - reviewComment = 审核备注
   - reviewedAt = now()
3. 根据 decision 更新申请状态：
   - approve → APPROVED（可进入 RAG 知识库）
   - reject → REJECTED
   - needMoreInfo → NEED_MORE_INFO
4. 返回更新后的申请记录
```

### 4.3 补交材料

```
POST /api/user/author/application/resubmit
Body: {
    applyReason: "补充说明",
    contentDirection: "擅长内容方向",
    introduction: "个人简介",
    sampleLinks: ["https://..."]
}
```

**处理流程**：

```
1. 校验用户存在 NEED_MORE_INFO 状态的申请
2. 更新申请记录
3. 状态重新变为 PENDING
4. 返回更新后的申请记录
```

## 5. 接口列表

### 5.1 用户侧接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/author/application` | POST | 提交作者申请 |
| `/api/user/author/application` | GET | 获取我的最新申请 |
| `/api/user/author/applications/page` | GET | 分页查询我的申请记录 |

### 5.2 管理端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/author/applications/page` | GET | 分页查询作者申请 |
| `/api/admin/author/applications/{id}` | GET | 获取申请详情 |
| `/api/admin/author/applications/{id}/review` | POST | 审核申请 |
| `/api/admin/author/applications/{id}/repair` | POST | 修复申请（管理员补录） |

### 5.3 公开接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/public/author/profile/{userId}` | GET | 获取作者公开资料 |

## 6. 服务层组件

### 6.1 UserAuthorApplicationService

| 方法 | 说明 |
|------|------|
| submitApplication(request) | 提交作者申请 |
| getLatestApplication() | 获取最新申请 |
| pageMyApplications(query) | 分页查询我的申请 |

### 6.2 SysAuthorApplicationAdminService

| 方法 | 说明 |
|------|------|
| pageApplications(query) | 分页查询申请 |
| getApplication(id) | 获取申请详情 |
| reviewApplication(id, request) | 审核申请 |
| repairApplication(id, request) | 修复申请 |

### 6.3 AuthorPermissionService

| 方法 | 说明 |
|------|------|
| isAuthor(userId) | 判断用户是否为作者 |
| canEnterRag(userId) | 判断用户是否可进入 RAG 知识库 |

## 7. 作者权限

### 7.1 作者标识

通过 `SysAuthorApplication.apply_status = APPROVED` 判断用户是否为正式作者。

### 7.2 RAG 知识库权限

作者申请通过后，用户可进入 RAG 知识库：

```java
public boolean canEnterRag(Long userId) {
    SysAuthorApplication application = repository.findApprovedByUserId(userId);
    return application != null;
}
```

### 7.3 作者权益

| 权益 | 说明 |
|------|------|
| RAG 知识库 | 可将内容向量存入知识库 |
| 发布文章 | 可发布文章（受等级限制） |
| 高级功能 | 可使用高级编辑器等 |

## 8. 关键设计

### 8.1 申请唯一性

同一用户同一时间只能有一条 PENDING 或 APPROVED 状态的申请：

```java
public void validateNoActiveApplication(Long userId) {
    boolean exists = repository.existsPendingOrApproved(userId);
    ExceptionThrowerCore.throwBusinessIf(exists,
        ResultErrorCode.ILLEGAL_ARGUMENT,
        "您已有待审核或已通过的作者申请");
}
```

### 8.2 审核权限

仅超级管理员或具备作者审核权限的角色可审核申请。

### 8.3 审核日志

审核操作记录于审计日志（sys_audit_log），包含：

- 审核人ID
- 申请ID
- 原状态
- 新状态
- 审核备注
- 审核时间

### 8.4 公开作者资料

作者可设置公开的个人资料页：

```java
// 查询可进入 RAG 知识库的作者
List<SysUser> listPublicProfilesForRag(int limit);

// 查询指定用户公开资料
SysUser findPublicProfileForRag(Long userId);
```
