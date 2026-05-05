# 文件-聊天门面

## 1. 概述

`FileChatFacadeService` 是 File 模块对 Chat 模块暴露的稳定 facade（门面），封装了文件业务引用与聊天消息的绑定/解绑逻辑，是 **File 模块与 Chat 模块之间的唯一耦合点**。

## 2. 核心职责

| 职责 | 说明 |
|------|------|
| 引用校验 | 校验文件引用是否可用于聊天消息发送 |
| 引用绑定 | 将临时引用或未绑定消息的引用收口到指定消息 ID |
| 引用释放 | 聊天消息删除时释放文件引用并触发生命周期同步 |

## 3. 接口定义

```java
public interface FileChatFacadeService {

    /**
     * 校验文件业务引用可被聊天消息消费，并返回真实文件信息。
     */
    FileInfo requireSendableChatFile(Long userId, Long businessId, String chatReferenceType);

    /**
     * 将临时文件引用或未绑定的聊天引用收口到指定聊天消息ID。
     */
    FileBusinessInfo bindChatMessageReference(Long userId,
                                              Long businessId,
                                              Long messageId,
                                              String chatReferenceType,
                                              String chatCategory);

    /**
     * 释放指定业务引用下的全部文件绑定，并同步回刷文件生命周期。
     */
    void releaseReferences(String referenceType, Long referenceId);

    /**
     * 按 ID 查询文件信息；文件不存在时返回 null。
     */
    FileInfo getFileInfo(Long fileId);
}
```

## 4. 核心流程

### 4.1 引用校验

```
聊天发送前
    │
    ▼
requireSendableChatFile(userId, businessId, chatReferenceType)
    │
    ├─ 查询 FileBusinessInfo
    ├─ 校验归属权（userId 非 null 时必须匹配）
    ├─ 校验引用类型（必须是 temp 或 chat_message）
    ├─ 校验聊天引用未绑定消息（referenceId 为 null 或 ≤ 0）
    ├─ 校验文件状态为 NORMAL
    │
    ▼
返回 FileInfo
```

**校验失败场景**：
| 错误 | 原因 |
|------|------|
| 引用不存在 | businessId 对应记录被删除 |
| 无权使用 | 引用不属于当前用户 |
| 引用类型不合法 | 非 temp 或 chat_message 类型 |
| 已绑定消息 | chat_message 引用已存在 referenceId |
| 文件不可发送 | 文件状态非 NORMAL |

### 4.2 引用绑定

```
聊天消息发送时
    │
    ▼
bindChatMessageReference(userId, businessId, messageId, chatReferenceType, chatCategory)
    │
    ├─ 校验源引用可被消费
    ├─ 校验文件状态正常
    │
    ├─ 查询是否已存在该消息的绑定引用（幂等）
    │   │
    │   ├─ 不存在 → 创建新引用
    │   └─ 存在 → 复用已有引用
    │
    ├─ 若源引用 ≠ 新绑定，删除源引用
    │
    ├─ 刷新文件元数据引用计数
    │
    ▼
返回 FileBusinessInfo
```

**幂等保证**：
- 唯一键 `uk_file_user_ref(file_id, user_id, reference_type, reference_id)` 防止重复创建
- `DuplicateKeyException` 时降级为查询已有引用

**引用类型转换**：
```
temp (临时引用) → chat_message (聊天消息引用)
chat_message (未绑定消息) → chat_message (绑定消息)
```

### 4.3 引用释放

```
聊天消息删除时
    │
    ▼
releaseReferences(referenceType, referenceId)
    │
    ├─ 查询该业务引用下的所有文件引用
    ├─ 批量删除引用记录
    ├─ 逐个触发文件生命周期同步
    │   │
    │   ├─ 重算引用计数
    │   ├─ 引用归零 → 标记待物理删除
    │   └─ 执行物理回收
    │
    ▼
完成
```

## 5. 设计原则

### 5.1 事务边界

所有变更操作（绑定/解绑）在统一事务边界内完成，保证文件业务引用与消息的原子性。

### 5.2 幂等处理

- 绑定操作：重复绑定同一消息时直接返回已有引用，不重复创建
- 解绑操作：查询无引用时直接跳过

### 5.3 生命周期联动

- 绑定时调用 `refreshReferenceMetadata` 刷新引用计数，驱动文件状态更新
- 解绑时调用 `syncFileAfterReferenceRemoval` 触发引用归零回收判断

## 6. 公开访问代理

`PublicFileAccessService` 处理公开文件的访问代理，带文章权限校验：

```java
public FileContentVO getFileContent(Long fileId) {
    // 1. 校验文件存在且状态正常
    // 2. 查询文件关联的业务记录
    // 3. 若存在文章引用，校验用户是否有文章访问权限
    // 4. 无权访问则抛出 FORBIDDEN
    // 5. 从存储服务下载文件内容
    // 6. 返回文件流、文件名、MIME 类型
}
```

**访问控制逻辑**：
- 文件无文章引用 → 允许公开访问
- 文件有文章引用 → 用户必须有至少一篇文章的访问权限

## 7. 相关接口

| 接口 | 路径 | 说明 |
|------|------|------|
| 代理访问文件 | `GET /api/public/files/{fileId}` | 带文章权限校验的文件访问 |