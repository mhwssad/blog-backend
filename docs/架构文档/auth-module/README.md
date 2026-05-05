# Auth 模块架构文档

本文档收集了 Auth 模块（认证授权）完整的架构设计文档。

## 文档索引

| 文档 | 说明 |
|------|------|
| [00-auth-module-overview.md](./00-auth-module-overview.md) | Auth 模块总览：定位、分层、目录结构、能力矩阵 |
| [auth-data-model.md](./auth-data-model.md) | Auth 数据模型：数据库表结构、实体映射、枚举定义 |
| [auth-login-flow.md](./auth-login-flow.md) | Auth 登录流程：账号登录、邮箱验证码登录、Token 刷新 |
| [auth-rbac-system.md](./auth-rbac-system.md) | Auth RBAC 系统：角色管理、菜单权限、权限分配 |
| [auth-experience-system.md](./auth-experience-system.md) | Auth 经验等级体系：经验来源、等级计算、事件驱动 |
| [auth-notice-system.md](./auth-notice-system.md) | Auth 通知体系：通知类型、投递机制、用户偏好 |
| [auth-author-system.md](./auth-author-system.md) | Auth 作者申请体系：申请流程、审核管理、状态流转 |

## 核心能力概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Auth 模块能力                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   账号管理         │  │   认证授权       │  │   RBAC 权限      │          │
│  │   登录/注册/邮箱   │  │   JWT/Redis Token│  │   角色/菜单/用户  │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   经验等级体系     │  │   通知体系       │  │   作者申请       │          │
│  │   经验值/等级/事件 │  │   通知/投递/偏好  │  │   申请/审核/状态  │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   系统配置        │  │   审计日志       │  │   接管登录        │          │
│  │   配置项/体验调整  │  │   操作日志/清理  │  │   超级管理员接管  │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 子域划分

| 子域 | 包路径 | 核心职责 |
|------|--------|----------|
| account | `module/auth/account` | 用户账号、认证登录、Token 管理、个人资料 |
| rbac | `module/auth/rbac` | 角色管理、菜单权限、用户-角色-菜单关联 |
| experience | `module/auth/experience` | 经验值管理、等级计算、经验流水 |
| notice | `module/auth/notice` | 系统通知、用户通知、通知偏好设置 |
| author | `module/auth/author` | 作者申请、申请审核、作者权限 |
| config | `module/auth/config` | 系统配置、体验调整 |
| audit | `module/auth/audit` | 审计日志、操作日志 |

## 快速导航

### 开发者视角

1. **理解登录流程** → 参考 [auth-login-flow.md](./auth-login-flow.md)
2. **查看数据表结构** → 参考 [auth-data-model.md](./auth-data-model.md)
3. **了解 RBAC 权限体系** → 参考 [auth-rbac-system.md](./auth-rbac-system.md)
4. **扩展经验来源** → 参考 [auth-experience-system.md](./auth-experience-system.md)
5. **理解通知投递** → 参考 [auth-notice-system.md](./auth-notice-system.md)
6. **作者申请审核** → 参考 [auth-author-system.md](./auth-author-system.md)

### 运营视角

1. **用户管理** → 后台 `/api/admin/users/page`
2. **角色管理** → 后台 `/api/admin/sys/roles/page`
3. **菜单管理** → 后台 `/api/admin/sys/menus/page`
4. **作者申请审核** → 后台 `/api/admin/author/applications/page`
5. **系统通知** → 后台 `/api/admin/notices/page`
6. **经验日志** → 后台 `/api/admin/experience/logs/page`
7. **审计日志** → 后台 `/api/admin/audit/logs/page`
8. **配置项管理** → 后台 `/api/admin/sys/configs/page`

### 相关文档

- [项目代码编写规范](../../项目代码编写规范.md)
- [项目结构规范](../../项目结构规范.md)
- [第二期任务导航](../../tasks/README.md)
