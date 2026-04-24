# Auth 模块 Repository 迁移记录

## 完成状态

- **状态**：已完成
- **完成时间**：2026-04-01
- **轮次**：第3轮
- **复杂度**：中
- **前置依赖**：无（基础模块，被广泛依赖）

## 本轮完成内容

### 1. 新增 9 个 Repository 接口与 9 个实现

| Repository 接口 | 对应实体 | 说明 |
|---|---|---|
| `SysUserRepository` | `SysUser` | 收口用户名/邮箱读取、登录信息更新、唯一性校验、管理端分页 |
| `SysRoleRepository` | `SysRole` | 收口角色编码读取、唯一性校验、管理端分页 |
| `SysMenuRepository` | `SysMenu` | 收口权限/菜单读取、菜单树查询、子菜单查询与删除前校验 |
| `SysConfigRepository` | `SysConfig` | 收口配置键读取、唯一性校验、管理端分页 |
| `SysLogRepository` | `SysLog` | 收口日志分页、按条件清理，并保留 `REQUIRES_NEW` 日志写入入口 |
| `SysNoticeRepository` | `SysNotice` | 收口通知分页、收件箱分页、未读统计、全员未读查询 |
| `SysUserNoticeRepository` | `SysUserNotice` | 收口通知投递关系查询、删除、存在性判断与最新关系读取 |
| `SysRoleMenuRepository` | `SysRoleMenu` | 收口角色菜单查询与按角色/菜单清理 |
| `SysUserRoleRepository` | `SysUserRole` | 收口用户角色查询与按用户/角色清理 |

### 2. 已切换到 Repository 的 8 个业务服务

1. `AuthServiceImpl`
2. `SysUserAdminServiceImpl`
3. `SysRoleAdminServiceImpl`
4. `SysMenuAdminServiceImpl`
5. `SysConfigAdminServiceImpl`
6. `SysLogAdminServiceImpl`
7. `SysNoticeAdminServiceImpl`
8. `UserNoticeInboxServiceImpl`

### 3. 已同步更新测试

- 已将以下测试改为基于 Repository mock，而不是 `lambdaQuery/lambdaUpdate` 链式 stub：
- `AuthServiceImplTest`
- `SysUserAdminServiceImplTest`
- `SysRoleAdminServiceImplTest`
- `SysMenuAdminServiceImplTest`
- `SysConfigAdminServiceImplTest`
- `SysLogAdminServiceImplTest`
- `SysNoticeAdminServiceImplTest`
- `UserNoticeInboxServiceImplTest`

## 保留项与遗留问题（2026-04-24 更新）

- [x] ~~`SysUserService` 仍被外部模块注入~~ → 已将 6 个外部调用方替换为 `SysUserRepository`，旧薄服务已删除。
- [x] ~~`SysRoleService`/`SysMenuService` 仍被 `AuthUserDetailsServiceImpl` 注入~~ → 已替换为 `SysRoleRepository`/`SysMenuRepository`，旧薄服务已删除。
- [x] ~~`SysLogService` 仍被 `SysLogAspect` 引用~~ → 已替换为 `SysLogRepository.saveLog()`（含 `REQUIRES_NEW`），旧薄服务已删除。
- [x] ~~`SysNoticeService`/`SysUserNoticeService` 仍被 `FollowNoticeServiceImpl` 引用~~ → 已替换为 `SysNoticeRepository`/`SysUserNoticeRepository`，旧薄服务已删除。
- [x] ~~`SysRoleMenuServiceImpl`/`SysUserRoleServiceImpl` 含 lambdaQuery 调用~~ → 随旧薄服务删除已清理。
- **`SysConfigService` 保留为真正业务服务**（含缓存逻辑 `getValueOrDefault`/`evictConfigCache`，被 `SysConfigAdminServiceImpl`、`ChatMessageGovernanceServiceImpl`、`IpRateLimitFilter` 注入），不删除。

## 验证结果

### 编译验证

```bash
mvn -q -DskipTests compile
```

- 结果：通过

### 测试验证

- 直接执行 `mvn test -Dtest=...` 时，会被仓库内其他模块的存量测试编译错误阻塞，当前不适合作为 auth 单模块验收方式。
- 本轮已单独构建 auth 测试 classpath，并仅编译/执行以下 8 个测试类：
- `AuthServiceImplTest`
- `SysUserAdminServiceImplTest`
- `SysRoleAdminServiceImplTest`
- `SysMenuAdminServiceImplTest`
- `SysConfigAdminServiceImplTest`
- `SysLogAdminServiceImplTest`
- `SysNoticeAdminServiceImplTest`
- `UserNoticeInboxServiceImplTest`
- 结果：共 `51` 个用例，`51` 个通过，`0` 失败。

## 后续动作

1. 在 `follow`、`article`、`content`、`chat` 等模块的 Repository 轮次中，逐步把对 `SysUserService` 的跨模块依赖替换为 `SysUserRepository`。
2. 当 `SysUserService`、`SysRoleService`、`SysMenuService` 等旧薄服务不再被外部模块注入后，再执行对应删除动作。
3. 仓库级 `mvn test` 仍需另外修复 chat/file 的存量测试编译问题，避免后续模块验收继续被无关失败阻塞。
