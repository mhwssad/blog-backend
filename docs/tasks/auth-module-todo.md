# Auth 模块待办清单

本文档用于收口 auth 模块接下来要持续推进的任务，避免"知道有缺口，但每次都重新梳理一遍"。本清单按 2026-03-30 当前代码状态整理。

## 1. 模块结构概览

```
module/auth/
├── controller/          (8个控制器)
│   ├── AuthController              - 认证主链路(登录/注册/邮箱登录/刷新/退出/当前用户)
│   ├── SysUserAdminController      - 后台用户管理(分页/详情/创建/修改/删除/状态/密码重置/角色分配)
│   ├── SysRoleAdminController      - 后台角色管理(分页/详情/创建/修改/删除/状态/菜单分配)
│   ├── SysMenuAdminController      - 后台菜单管理(树/详情/创建/修改/删除)
│   ├── SysConfigAdminController    - 后台配置管理(分页/详情/创建/修改/删除/按键查询)
│   ├── SysNoticeAdminController    - 后台通知管理(分页/详情/创建/修改/发布/撤回/删除)
│   ├── SysLogAdminController       - 后台日志管理(分页/详情/删除/批量清理)
│   └── UserNoticeController        - 用户通知收件箱(分页/详情/未读数/已读/全部已读)
├── service/             (18个接口 + 18个实现)
│   ├── AuthService              - 认证主链路(8个方法)
│   ├── SysUserAdminService      - 用户管理(9个方法)
│   ├── SysRoleAdminService      - 角色管理(8个方法)
│   ├── SysMenuAdminService      - 菜单管理(5个方法)
│   ├── SysConfigAdminService    - 配置管理(6个方法)
│   ├── SysNoticeAdminService    - 通知管理(7个方法)
│   ├── SysLogAdminService       - 日志管理(4个方法)
│   ├── UserNoticeInboxService   - 收件箱(5个方法)
│   └── 基础仓储(SysUserService, SysRoleService, SysMenuService, SysUserRoleService,
│                SysRoleMenuService, SysConfigService, SysLogService, SysNoticeService,
│                SysUserNoticeService, AuthUserDetailsService)
├── token/               (TokenManager接口 + JwtTokenManager实现)
├── authentication/      (EmailCode认证 Provider + Token)
├── convert/             (5个MapStruct映射器)
└── model/               (6个公开DTO + 16个管理侧DTO)
```

## 2. 功能完成度评估

### 2.1 接口方法完成情况

| 服务                     | 方法数  | 已实现 | 状态     |
|------------------------|------|-----|--------|
| AuthService            | 8    | 8   | ✅ 全部完成 |
| SysUserAdminService    | 9    | 9   | ✅ 全部完成 |
| SysRoleAdminService    | 8    | 8   | ✅ 全部完成 |
| SysMenuAdminService    | 5    | 5   | ✅ 全部完成 |
| SysConfigAdminService  | 6    | 6   | ✅ 全部完成 |
| SysNoticeAdminService  | 7    | 7   | ✅ 全部完成 |
| SysLogAdminService     | 4    | 4   | ✅ 全部完成 |
| UserNoticeInboxService | 5    | 5   | ✅ 全部完成 |
| 基础仓储(10个)              | 各1-4 | 全部  | ✅ 全部完成 |

**结论: auth 模块所有接口方法均已完整实现，不存在缺失的方法。**

## 3. 本轮已完成

- [x] 实现认证主链路（登录/注册/邮箱验证码/刷新/退出/当前用户/菜单树）。
- [x] 实现后台用户/角色/菜单/配置/通知/日志全量管理接口。
- [x] 实现用户通知收件箱（分页/详情/未读数/已读/全部已读）。
- [x] 将认证域 DTO 转换收口到 `AuthModelMapper` 等 5 个 MapStruct 映射器。
- [x] 补入通知发布投递与用户通知收件箱服务级测试。
- [x] 补入认证主链路服务级测试（登录规范化/邮箱验证码/刷新令牌/幂等退出/当前用户装配）。
- [x] 补入注册边界服务级测试（重复用户名/邮箱/手机号阻断、字段规范化、默认状态回填）。
- [x] 补入邮箱验证码异常边界测试（禁用账号禁止发送、邮件发送失败不缓存验证码）。
- [x] 补入 RBAC 分配边界服务级测试（角色菜单分配、用户角色分配的空ID/无效目标拦截）。
- [x] 补入后台接口权限控制 WebMvc 测试（角色/用户/通知/菜单/配置/日志管理关键权限点）。
- [x] 补入通知收件箱未读统计聚合测试与已读幂等补建。
- [x] 补入 `JwtTokenManagerTest` 与 `TokenAuthenticationFilterTest`。
- [x] 补入 `IpRateLimitFilterTest`，覆盖全局 IP 限流过滤器。
- [x] 补入 `SysUserAdminServiceImplTest` 与 `SysRoleAdminServiceImplTest` 服务级测试。
- [x] 补入 `SysNoticeAdminServiceImplTest` 服务级测试。
- [x] 补入 `SysConfigAdminServiceImplTest`、`SysMenuAdminServiceImplTest`、`SysLogAdminServiceImplTest` 服务级测试。
- [x] 补入 `SysConfigServiceImplTest`、`SysRoleMenuServiceImplTest`、`SysUserRoleServiceImplTest` 基础仓储测试。
- [x] 为认证主链路补入“登录失败锁定 + 并发注册唯一键兜底”能力，并同步补充默认系统配置项。
- [x] 补入 `EmailCodeAuthenticationProviderTest` 与 `RedisTokenManagerTest`，覆盖过期验证码、Redis 刷新令牌失效和单账号会话挤下线策略。
- [x] 补入通知中心并发幂等回归，验证 `markAllRead` 在通知-用户唯一键竞争下仍可自动回读既有关系。

## 4. 现有测试文件 (17个)

| 测试文件                                  | 覆盖范围                              |
|---------------------------------------|-----------------------------------|
| `AuthServiceImplTest`                 | 登录/注册/邮箱验证码/刷新/退出/当前用户装配          |
| `EmailCodeAuthenticationProviderTest` | 验证码过期/禁用账号/成功登录后删除验证码             |
| `SysUserAdminServiceImplTest`         | 用户管理 CRUD + 状态切换 + 密码重置 + 角色分配    |
| `SysRoleAdminServiceImplTest`         | 角色管理 CRUD + 状态切换 + 菜单分配 + 级联清理    |
| `SysNoticeAdminServiceImplTest`       | 通知管理 CRUD + 发布/撤回 + 目标用户校验        |
| `UserNoticeInboxServiceImplTest`      | 收件箱分页/详情/未读/已读/全部已读               |
| `AuthAdminControllerSecurityTest`     | 后台管理接口 WebMvc 权限拦截                |
| `JwtTokenManagerTest`                 | JWT 令牌生成/解析/刷新/失效                 |
| `RedisTokenManagerTest`               | Redis 令牌换发、旧令牌失效与单账号会话策略          |
| `TokenAuthenticationFilterTest`       | 认证过滤器链路                           |
| `IpRateLimitFilterTest`               | 全局 IP 限流                          |
| `SysConfigAdminServiceImplTest`       | 配置 CRUD + 按键查询 + 缓存淘汰             |
| `SysMenuAdminServiceImplTest`         | 菜单树构建 + 创建/更新/删除 + 子节点检查 + 角色菜单清理 |
| `SysLogAdminServiceImplTest`          | 日志分页 + 详情 + 删除 + 按条件清理            |
| `SysConfigServiceImplTest`            | 配置缓存读取与失效                         |
| `SysRoleMenuServiceImplTest`          | 角色菜单替换与级联清理                       |
| `SysUserRoleServiceImplTest`          | 用户角色替换与级联清理                       |

## 5. 下一批高优先级

### 5.1 缺少服务级测试的管理服务

- [x] `SysConfigAdminServiceImpl` 服务级测试 - 配置 CRUD + 按键查询 + 缓存淘汰
- [x] `SysMenuAdminServiceImpl` 服务级测试 - 菜单树构建 + 创建/更新/删除 + 子节点检查 + 角色菜单清理
- [x] `SysLogAdminServiceImpl` 服务级测试 - 日志分页 + 详情 + 删除 + 批量清理条件校验

### 5.2 缺少服务级测试的基础仓储

- [x] `SysConfigServiceImpl` 测试 - `getValueByKey` Redis 缓存 + `evictConfigCache` 缓存失效
- [x] `SysRoleMenuServiceImpl` 测试 - `replaceRoleMenus` 替换逻辑 + `removeByMenuId` 级联
- [x] `SysUserRoleServiceImpl` 测试 - `replaceUserRoles` 替换逻辑 + `removeByRoleId` 级联

### 5.3 认证主链路扩展边界

- [x] 登录失败次数锁定/账户冻结场景（若业务需要）
  当前已补登录失败临时锁定，默认连续失败 `5` 次锁定 `15` 分钟；账号冻结继续沿用既有 `status=0` 禁用语义。
- [x] 邮箱验证码过期后重发 + 过期验证码使用场景
  当前口径已明确为“验证码 5 分钟过期、60 秒发送频控、过期后可重新申请新码”，并已补入 provider 级回归测试。
- [x] 并发注册同一用户名/邮箱/手机号的竞争验证
  当前已由数据库唯一约束兜底，并在服务层把 `DuplicateKeyException` 重新翻译为用户名/邮箱/手机号重复提示。
- [x] 令牌刷新时旧令牌失效策略（若需要 Redis 模式）
  当前已明确为“`redis-token` 模式下刷新会回收旧 access/refresh，`jwt` 模式保持纯无状态换发”，并已补测试。

## 6. 中期一致性补强

- [x] 核对 JWT 纯无状态模式下退出登录的实际行为（当前 `invalidateToken` 为空操作）
  结论：当前 `jwt` 模式下退出仅负责前端幂等清理入口，服务端不回收已签发 JWT；接口文档已同步说明。
- [x] 核对角色删除时关联的用户-角色、角色-菜单级联清理是否完整
  当前 `deleteRole` 已统一收口 `sys_role_menu` 和 `sys_user_role` 级联清理，服务级测试已覆盖。
- [x] 核对菜单删除时关联的角色-菜单级联清理是否完整
  当前 `deleteMenu` 已先校验无子节点，再清理 `sys_role_menu` 后删除菜单，服务级测试已覆盖。
- [x] 核对通知发布后目标用户的投递完整性（全量用户 vs 指定用户）
  当前已明确为“全员通知不预建 `sys_user_notice`，指定用户通知在发布时批量落投递关系”，并已补对应测试。
- [x] 核对全部已读时与全局通知的幂等补建逻辑在高并发下的行为
  当前已明确使用通知-用户唯一键 + `DuplicateKeyException` 回查兜底，`markAllRead` 并发补建已补回归。
- [x] 补充高成本方法的 Javadoc 注释（如 `pageMyNotices` 全局+定向合并逻辑）
  当前认证、通知、RBAC 关键编排方法已完成一轮注释收口。
- [x] 评估是否需要引入 Redis 模式令牌管理实现（支持真正的令牌失效和单点登录）
  结论：当前已具备 `redis-token` 实现，可支持刷新回收旧令牌与单账号会话挤下线；是否默认切换取决于部署成本和 Redis 可用性。

## 7. 中长期基础设施

- [x] 评估是否需要补白名单 IP 跳过限流能力
  结论：当前先复用现有 `security.ignoreUrls`/网关白名单，不在 auth 内单独追加一套 IP 白名单配置。
- [x] 评估是否需要按接口维度限流（如登录/注册接口更严格阈值）
  结论：当前先维持“全局 IP 限流 + 邮箱验证码发送频控”组合；若登录攻击明显增多，再单独为登录/注册补用户级或接口级限流。
- [x] 评估是否需要更细粒度审计日志（操作人、操作内容快照）
  结论：当前系统日志已覆盖基础请求链路，后续若进入合规/审计要求更高阶段，再补操作前后快照和结果字段。
- [x] 当用户量增长后，评估配置缓存、菜单缓存和权限缓存策略
  结论：当前配置缓存已存在，菜单/权限缓存暂不引入；等权限查询成为热点后，再评估按用户维度缓存及失效策略。

## 8. 完成标志

- 认证主链路、RBAC 管理、通知收件箱三条主线具备完整的服务级回归覆盖。
- 配置/菜单/日志管理具备基础自动化验证。
- 权限相关接口行为与 API 文档保持一致。
