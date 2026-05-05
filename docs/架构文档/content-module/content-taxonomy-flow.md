# 分类标签模块流程

## 1. 概述

分类标签模块（`taxonomy` 子域）负责文章分类树维护与标签管理。

## 2. 核心实体

### SysCategory（分类）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| parentId | Long | 父分类ID（0=根） |
| name | String | 分类名称 |
| code | String | 分类编码（唯一） |
| type | String | 类型（article） |
| ancestors | String | 祖先路径（逗号分隔） |
| level | Integer | 层级深度 |
| sortOrder | Integer | 排序值 |
| icon | String | 分类图标 |
| description | String | 分类描述 |
| status | Integer | 状态（0=禁用，1=启用） |

### SysTag（标签）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 标签名称 |
| color | String | 标签颜色（十六进制） |

## 3. 服务接口

### CategoryAdminService（分类管理）

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/content/taxonomy/service/CategoryAdminService.java#L1-17
public interface CategoryAdminService {
    List<CategoryTreeVO> listCategoryTree();                  // 查询分类树
    CategoryAdminVO getCategory(Long id);                    // 获取详情
    CategoryAdminVO createCategory(CategorySaveRequest request);  // 创建分类
    CategoryAdminVO updateCategory(Long id, CategorySaveRequest request);  // 更新分类
    void updateStatus(Long id, Integer status);             // 启用/禁用
    void deleteCategory(Long id);                           // 删除分类
}
```

### TagAdminService（标签管理）

- `pageTags(query)` - 分页查询标签
- `getTag(id)` - 获取标签详情
- `createTag(request)` - 创建标签
- `updateTag(id, request)` - 更新标签
- `deleteTag(id)` - 删除标签

## 4. 核心流程

### 4.1 创建分类

```
管理员创建分类
        │
        ▼
校验分类类型（仅支持 article）
        │
        ▼
校验编码唯一性
        │
        ▼
校验父分类合法性
   ├── 父分类不能为自身
   └── 父分类不能是自身的子孙节点（防止环）
        │
        ▼
计算层级
   level = parent == null ? 1 : parent.level + 1
        │
        ▼
计算祖先路径
   ancestors = parent == null ? "0" : parent.ancestors + "," + parent.id
        │
        ▼
保存分类
        │
        ▼
返回分类信息
```

### 4.2 更新分类（移动）

```
管理员更新分类（可能涉及移动）
        │
        ▼
校验编码唯一性（排除自身）
        │
        ▼
校验父分类合法性（同创建流程）
        │
        ▼
更新分类基本信息
        │
        ▼
递归刷新子分类层级和祖先链
   children.level = parent.level + 1
   children.ancestors = parent.ancestors + "," + parent.id
        │
        ▼
返回更新后分类
```

### 4.3 删除分类

```
管理员删除分类
        │
        ▼
检查是否存在子分类
        │存在 → 抛出异常，不可删除
        ▼不存在
        ▼
检查是否已绑定文章
        │已绑定 → 抛出异常，不可删除
        ▼未绑定
        ▼
删除分类
```

### 4.4 标签管理

```
创建标签：校验名称唯一 → 保存
更新标签：校验名称唯一（排除自身） → 保存
删除标签：校验未被使用 → 删除
```

## 5. 分类树结构

分类采用**邻接表 + 祖先路径**的混合模型：

```
ancestors = "0,1,5"  表示根→一级→二级
level = 3             表示该节点在第3层
```

**树构建流程**：
1. 按 type 查询所有分类，按 sortOrder + id 排序
2. 构建 Map<id, CategoryTreeVO>
3. 遍历构建父子关系（parentId = 0 为根节点）
4. 返回根节点列表

## 6. 约束规则

| 约束 | 说明 |
|------|------|
| 类型限制 | 仅支持 article 类型分类 |
| 编码唯一 | 同类型下 code 必须唯一 |
| 层级上限 | 未限制，但 ancestors 字段限制了深度 |
| 父节点限制 | 不能自关联，不能挂到子孙节点 |
| 删除前置条件 | 不能有子分类，不能有绑定文章 |

## 7. API 路由

| 路由 | 方法 | 说明 | 鉴权 |
|------|------|------|------|
| `/api/sys/category` | GET | 查询分类树 | 管理员 |
| `/api/sys/category/{id}` | GET | 分类详情 | 管理员 |
| `/api/sys/category` | POST | 创建分类 | 管理员 |
| `/api/sys/category/{id}` | PUT | 更新分类 | 管理员 |
| `/api/sys/category/{id}/status` | PUT | 启用/禁用 | 管理员 |
| `/api/sys/category/{id}` | DELETE | 删除分类 | 管理员 |
| `/api/sys/tag` | GET | 分页查询标签 | 管理员 |
| `/api/sys/tag/{id}` | GET | 标签详情 | 管理员 |
| `/api/sys/tag` | POST | 创建标签 | 管理员 |
| `/api/sys/tag/{id}` | PUT | 更新标签 | 管理员 |
| `/api/sys/tag/{id}` | DELETE | 删除标签 | 管理员 |
| `/api/public/category/tree` | GET | 前台分类树 | 公开 |
| `/api/public/tags` | GET | 前台标签列表 | 公开 |

## 8. 相关文档

- [Content 模块总览](./00-content-module-overview.md)