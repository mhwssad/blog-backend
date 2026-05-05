# 收藏模块流程

## 1. 概述

收藏模块（`collection` 子域）负责用户收藏夹维护与文章收藏管理。

## 2. 核心实体

### SysCollectionFolder（收藏夹）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 所属用户 |
| folderName | String | 收藏夹名称 |
| folderType | String | 类型（默认 article） |
| isDefault | Integer | 是否默认（1=默认） |
| isPublic | Integer | 是否公开（0=私密，1=公开） |
| sortOrder | Integer | 排序值 |
| collectionCount | Integer | 收藏数量（冗余） |

### SysCollection（收藏记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 收藏用户 |
| folderId | Long | 所属收藏夹 |
| targetId | Long | 收藏目标ID |
| targetType | String | 目标类型（article） |
| targetTitle | String | 目标标题（冗余） |
| targetUrl | String | 目标链接（冗余） |
| remark | String | 收藏备注 |

## 3. 服务接口

### UserCollectionService（用户侧）

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/content/collection/service/UserCollectionService.java#L1-17
public interface UserCollectionService {
    PageResult<CollectionFolderVO> pageFolders();           // 分页查询收藏夹
    CollectionFolderVO createFolder(CollectionFolderSaveRequest request);  // 创建收藏夹
    CollectionFolderVO updateFolder(Long id, CollectionFolderSaveRequest request);  // 更新收藏夹
    void deleteFolder(Long id);                            // 删除收藏夹
    PageResult<CollectionVO> pageCollections();            // 分页查询收藏记录
    void createCollection(CollectionSaveRequest request);   // 创建收藏
    void deleteCollection(Long id);                        // 删除收藏
}
```

### CollectionAdminService（后台管理）

- `pageCollections(query)` - 分页查询收藏记录
- `getCollection(id)` - 获取收藏详情
- `deleteCollection(id)` - 删除收藏记录

## 4. 核心流程

### 4.1 创建收藏夹

```
用户请求创建收藏夹
        │
        ▼
校验收藏夹名称（是否为空）
        │
        ▼
如果设为默认收藏夹 ──YES──> 取消同类型其他默认标记
        │NO
        ▼
保存收藏夹（collectionCount=0）
        │
        ▼
返回新建的收藏夹信息
```

### 4.2 创建收藏

```
用户收藏文章
        │
        ▼
校验目标类型（仅支持 article）
        │
        ▼
获取文章（校验文章可互动状态）
        │
        ▼
获取收藏夹（未指定则获取/创建默认收藏夹）
        │
        ▼
检查是否已收藏（用户+收藏夹+目标唯一）
        │已存在 → 直接返回（幂等）
        ▼不存在
        ▼
保存收藏记录（冗余标题、链接）
        │
        ▼
更新收藏夹计数（+1）
        │
        ▼
更新文章收藏数（+1）
        │
        ▼
发送通知（作者被收藏） ── 仅当文章作者≠收藏者
```

### 4.3 删除收藏夹

```
用户删除收藏夹
        │
        ▼
校验是否为默认收藏夹（默认夹不可删除）
        │
        ▼
查询该收藏夹下所有收藏记录
        │
        ▼
逐一回退文章收藏计数
        │
        ▼
删除所有收藏记录
        │
        ▼
删除收藏夹
```

## 5. 计数联动

收藏模块与文章模块存在计数联动：

| 操作 | 收藏夹 collectionCount | 文章 collectCount |
|------|------------------------|-------------------|
| 创建收藏 | folderId +1 | targetId +1 |
| 删除收藏 | folderId -1 | targetId -1 |
| 删除收藏夹 | - | 批量 -N |

## 6. API 路由

| 路由 | 方法 | 说明 | 鉴权 |
|------|------|------|------|
| `/api/user/collection/folders` | GET | 分页查询收藏夹 | 用户 |
| `/api/user/collection/folders` | POST | 创建收藏夹 | 用户 |
| `/api/user/collection/folders/{id}` | PUT | 更新收藏夹 | 用户 |
| `/api/user/collection/folders/{id}` | DELETE | 删除收藏夹 | 用户 |
| `/api/user/collections` | GET | 分页查询收藏 | 用户 |
| `/api/user/collections` | POST | 创建收藏 | 用户 |
| `/api/user/collections/{id}` | DELETE | 删除收藏 | 用户 |
| `/api/sys/collections` | GET | 后台分页查询 | 管理员 |

## 7. 相关文档

- [Content 模块总览](./00-content-module-overview.md)