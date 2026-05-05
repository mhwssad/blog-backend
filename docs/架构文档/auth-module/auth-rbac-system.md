# Auth RBAC 系统

## 1. 概述

RBAC（Role-Based Access Control）系统负责系统的权限管理，包括：

- **角色管理**：角色的增删改查、状态启用/禁用
- **菜单管理**：前端路由与按钮权限的维护
- **权限分配**：用户-角色、角色-菜单关联管理
- **数据权限**：不同角色可访问的数据范围控制

## 2. 核心数据模型

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   SysUser   │────▶│ SysUserRole │◀────│   SysRole   │
└─────────────┘     └─────────────┘     └─────────────┘
                                            │
                                            ▼
                                      ┌─────────────┐
                                      │ SysRoleMenu │
                                      └─────────────┘
                                            │
                                            ▼
                                      ┌─────────────┐
                                      │   SysMenu   │
                                      └─────────────┘
```

## 3. 角色管理

### 3.1 角色实体

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 角色名称 |
| code | String | 角色编码（唯一） |
| sort | Integer | 显示顺序 |
| status | Integer | 状态：1-正常，0-停用 |
| dataScope | Integer | 数据权限范围 |
| isDeleted | Integer | 逻辑删除标识 |

### 3.2 数据权限范围

| 值 | 标签 | 说明 |
|----|------|------|
| 1 | ALL | 所有数据 |
| 2 | DEPT_AND_CHILD | 部门及子部门数据 |
| 3 | DEPT | 本部门数据 |
| 4 | SELF | 本人数据 |
| 5 | CUSTOM | 自定义部门数据 |

### 3.3 角色管理接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/sys/roles/page` | GET | 分页查询角色 |
| `/api/admin/sys/roles/{id}` | GET | 获取角色详情（含菜单ID列表） |
| `/api/admin/sys/roles` | POST | 创建角色 |
| `/api/admin/sys/roles/{id}` | PUT | 更新角色 |
| `/api/admin/sys/roles/{id}/status` | PUT | 更新角色状态 |
| `/api/admin/sys/roles/{id}` | DELETE | 删除角色 |

### 3.4 角色删除流程

```
1. 校验角色是否存在
2. 删除角色-菜单关联（sys_role_menu）
3. 删除用户-角色关联（sys_user_role）
4. 逻辑删除角色记录
```

## 4. 菜单管理

### 4.1 菜单实体

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| parentId | Long | 父菜单ID |
| treePath | String | 父节点ID路径 |
| name | String | 菜单名称 |
| type | String | 类型：C-目录，M-菜单，B-按钮 |
| routeName | String | 路由名称 |
| routePath | String | 路由路径 |
| component | String | 组件路径 |
| perm | String | 按钮权限标识 |
| alwaysShow | Integer | 是否始终显示 |
| keepAlive | Integer | 是否开启缓存 |
| visible | Integer | 显示状态 |
| sort | Integer | 排序 |
| icon | String | 菜单图标 |
| redirect | String | 跳转路径 |
| params | Object | 路由参数 |

### 4.2 菜单类型说明

| 类型 | 标签 | 说明 |
|------|------|------|
| C | CATALOG | 目录（左侧菜单树的一级节点） |
| M | MENU | 菜单（具体的页面） |
| B | BUTTON | 按钮（页面内的操作按钮） |

### 4.3 菜单管理接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/sys/menus/page` | GET | 分页查询菜单 |
| `/api/admin/sys/menus/tree` | GET | 获取菜单树 |
| `/api/admin/sys/menus/{id}` | GET | 获取菜单详情 |
| `/api/admin/sys/menus` | POST | 创建菜单 |
| `/api/admin/sys/menus/{id}` | PUT | 更新菜单 |
| `/api/admin/sys/menus/{id}` | DELETE | 删除菜单 |

## 5. 权限分配

### 5.1 角色-菜单分配

```
PUT /api/admin/sys/roles/{roleId}/menus
Body: { menuIds: [1, 2, 3] }
```

**分配流程**：

```
1. 校验角色是否存在
2. 校验菜单ID列表有效性
3. 删除角色原有菜单关联
4. 批量插入新菜单关联
5. 返回 200 OK
```

### 5.2 用户-角色分配

```
PUT /api/admin/sys/users/{userId}/roles
Body: { roleIds: [1, 2, 3] }
```

**分配流程**：

```
1. 校验用户是否存在
2. 校验角色ID列表有效性
3. 删除用户原有角色关联
4. 批量插入新角色关联
5. 使该用户当前会话失效（强制重新登录）
6. 返回 200 OK
```

### 5.3 用户-角色审计分配（带审核）

```
PUT /api/admin/sys/users/{userId}/roles/audit
Body: { roleIds: [1, 2, 3], applyReason: "xxx" }
```

用于高敏感角色分配的审核流程。

## 6. 权限校验

### 6.1 权限获取流程

用户登录后，通过 `/api/auth/current-user-menus` 获取该用户的菜单权限：

```
1. 获取当前用户
2. 查询用户的角色列表
3. 查询角色关联的菜单ID列表
4. 查询菜单详情并构建菜单树
5. 返回菜单树结构
```

### 6.2 菜单树构建

```java
private List<AuthMenuInfo> buildMenuTree(List<SysMenu> menus) {
    // 1. 按 parentId 分组
    // 2. 递归构建子树
    // 3. 按 sort 排序
    // 4. 返回树形结构
}
```

## 7. 服务层组件

### 7.1 SysRoleAdminService

| 方法 | 说明 |
|------|------|
| pageRoles(query) | 分页查询角色 |
| getRole(id) | 获取角色详情 |
| createRole(request) | 创建角色 |
| updateRole(id, request) | 更新角色 |
| updateStatus(id, status) | 更新状态 |
| deleteRole(id) | 删除角色 |
| listMenuIds(roleId) | 查询角色菜单ID |
| assignMenus(roleId, menuIds) | 分配菜单 |

### 7.2 SysMenuAdminService

| 方法 | 说明 |
|------|------|
| pageMenus(query) | 分页查询菜单 |
| getMenuTree() | 获取菜单树 |
| getMenu(id) | 获取菜单详情 |
| createMenu(request) | 创建菜单 |
| updateMenu(id, request) | 更新菜单 |
| deleteMenu(id) | 删除菜单 |

## 8. 关键设计

### 8.1 关联工厂

`RbacAssociationFactory` 用于创建关联实体：

```java
// 创建角色-菜单关联
SysRoleMenu createRoleMenu(Long roleId, Long menuId)

// 创建用户-角色关联
SysUserRole createUserRole(Long userId, Long roleId)
```

### 8.2 唯一性校验

- 角色编码（code）在同一租户下唯一
- 菜单路由名称（routeName）在同级下唯一
- 用户名（username）全局唯一
- 邮箱（email）全局唯一

### 8.3 逻辑删除

所有核心数据（用户、角色、菜单）均采用逻辑删除：

- 删除时设置 `deleted_flag = 1` 或 `is_deleted = 1`
- 查询时默认过滤已删除记录
- 物理删除仅在数据清理时使用
