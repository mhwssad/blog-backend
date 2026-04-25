# Follow 模块待办清单

本文档用于收口 follow（粉丝关注）模块接下来要持续推进的任务，避免“知道有缺口，但每次都重新梳理一遍”。本清单按 2026-03-31
当前代码状态整理，并已同步到本轮 follow 后台/公开/通知联动落地后的状态。

## 1. 模块当前状态

**当前阶段: 用户侧主链路、后台治理、公开访客接口与新粉丝通知已落地，主页联动与缓存层保持审慎评估。**

```
已完成:
  ✅ 数据库表设计 (sys_user_follow)
  ✅ 域实体 (SysUserFollow.java)
  ✅ 基础 Mapper接口 + XML
  ✅ 基础服务接口 + 实现 (SysUserFollowService, extends IService/ServiceImpl)
  ✅ 用户侧控制器 (UserFollowController)
  ✅ 用户侧 DTO / VO / 分页查询模型
  ✅ 用户侧业务服务 (UserFollowService / UserFollowServiceImpl)
  ✅ MapStruct 映射器 (FollowModelMapper)
  ✅ 自定义 Mapper 查询（关注/粉丝分页、互关判断、关注数/粉丝数统计）
  ✅ 用户侧 API 文档 (docs/api文档/follow-api.md)
  ✅ 基础测试文件 (UserFollowControllerTest / UserFollowServiceImplTest)
  ✅ 后台管理接口与权限菜单
  ✅ 公开访客接口
  ✅ 新粉丝通知联动

待继续评估/推进:
  ⏳ 用户主页展示联动策略
  ⏳ Redis 缓存层
```

## 2. 数据库表设计

表 `sys_user_follow`，已定义在 `06_follow.sql` + `07_schema_repair.sql`：

| 字段                          | 类型      | 说明                          |
|-----------------------------|---------|-----------------------------|
| `id`                        | Long    | 主键                          |
| `follower_id`               | Long    | 关注者用户ID                     |
| `following_id`              | Long    | 被关注者用户ID                    |
| `follow_status`             | Integer | 0=已取关, 1=已关注                |
| `is_special_follow`         | Integer | 0=普通关注, 1=特别关注              |
| `source`                    | String  | 来源: manual/recommend/system |
| `follow_time`               | Date    | 最近关注时间                      |
| `unfollow_time`             | Date    | 最近取关时间                      |
| `remark`                    | String  | 备注                          |
| `created_at` / `updated_at` | Date    | 时间戳                         |

约束:

- 唯一键 `uk_follower_following (follower_id, following_id)`，防止重复关注。
- CHECK 约束 `chk_no_self_follow`，禁止自关注。
- 软取消设计: `follow_status` 在 `0/1` 之间切换，支持重新关注。

索引:

- `idx_follower_status_time`，支撑“我的关注”列表查询。
- `idx_following_status_time`，支撑“我的粉丝”列表查询。
- `idx_follow_pair_status`，支撑互关/状态检查。

## 3. 实现规则结论

- [x] 关注/取关行为是单向还是需要对方确认？
    - 当前实现为**单向关注**，无需对方确认。
- [x] 取关后重新关注的行为（复用原记录切换状态 vs 新建记录）
    - 当前实现为**复用原记录切换状态**。
- [x] 特别关注的语义与特殊能力（如专属动态/优先展示）
    - 当前先收口为**关注列表置顶筛选能力**，暂不扩展专属动态。
- [x] 是否允许自关注（数据库已禁止，业务层是否需要额外校验）
    - 当前业务层也已显式拦截自关注。
- [x] 粉丝数/关注数的统计口径（实时 COUNT vs 独立计数字段 vs 异步统计）
    - 当前采用**实时 COUNT**。
- [x] 关注关系与通知的联动（新粉丝是否通知被关注者）
    - 当前已落地**新粉丝通知**，并采用**事务提交后异步补写**，不让通知失败回滚关注主链路。
- [x] 是否需要后台管理能力（查看/清理异常关注关系）
    - 当前已落地**后台分页查询 + 异常关系清理**，用于治理历史脏数据和用户状态异常关系。
- [x] 关注关系与用户主页的联动（关注后是否影响主页展示内容）
    - 当前结论为**先保持解耦**；follow 模块只维护关系与通知，不直接驱动主页内容编排。

## 4. 功能落地状态

### 4.1 用户侧接口

- [x] **关注用户** `POST /api/user/follows/{userId}`
- [x] **取消关注** `DELETE /api/user/follows/{userId}`
- [x] **我的关注列表** `GET /api/user/follows`
- [x] **我的粉丝列表** `GET /api/user/fans`
- [x] **互关判断** `GET /api/user/follows/mutual?targetUserId={id}`
- [x] **关注数/粉丝数统计** `GET /api/user/follows/count`
- [x] **设置/取消特别关注** `PUT /api/user/follows/{userId}/special`
- [x] **设置备注** `PUT /api/user/follows/{userId}/remark`

### 4.2 后台管理接口

- [x] **关注关系分页** `GET /api/sys/follows`
- [x] **异常关注清理** `DELETE /api/sys/follows/clean`

### 4.3 公开接口

- [x] **查看用户关注列表** `GET /api/users/{userId}/follows`
- [x] **查看用户粉丝列表** `GET /api/users/{userId}/fans`

### 4.4 服务层实现

- [x] `UserFollowService` 接口设计（关注/取关/列表/互关/统计/特别关注/备注）
- [x] `UserFollowServiceImpl` 业务逻辑实现
- [x] 自定义 Mapper 查询（关注列表JOIN用户信息、互关判断、粉丝数/关注数统计）
- [x] MapStruct 映射器（实体→DTO转换）
- [x] DTO 设计（请求/响应/分页查询）
- [x] `FollowAdminService` / `PublicFollowService` / `FollowNoticeService` 配套实现

### 4.5 统计与缓存

- [x] 关注数/粉丝数统计方案（实时 COUNT / 用户表冗余字段 / Redis缓存）
- [x] 统计一致性保证（关注/取关时同步更新）
    - 当前因采用实时 COUNT，关注/取关只需维护关系状态一致性。
- [ ] Redis 缓存层
    - 当前结论：**暂不引入**。现阶段接口复杂度和热点规模不足以支撑额外缓存一致性成本。

### 4.6 通知联动

- [x] 新粉丝通知（被关注者收到提醒）
- [x] 通知失败不影响关注主链路
- [ ] 关注请求通知（如采用双向确认模式）
    - 当前结论：**不适用**，因为 follow 规则仍为单向关注。

## 5. 现有文件清单（核心文件已扩展）

| 文件                             | 路径                            | 内容                |
|--------------------------------|-------------------------------|-------------------|
| `UserFollowController.java`    | `module/follow/controller/`   | 用户侧关注接口           |
| `PublicFollowController.java`  | `module/follow/controller/`   | 公开访客接口            |
| `FollowAdminController.java`   | `module/follow/controller/`   | 后台管理接口            |
| `UserFollowServiceImpl.java`   | `module/follow/service/impl/` | 用户侧主链路 + 通知挂钩     |
| `PublicFollowServiceImpl.java` | `module/follow/service/impl/` | 公开访客分页查询          |
| `FollowAdminServiceImpl.java`  | `module/follow/service/impl/` | 后台分页与异常清理         |
| `FollowNoticeServiceImpl.java` | `module/follow/service/impl/` | 新粉丝通知补写           |
| `FollowModelMapper.java`       | `module/follow/convert/`      | 用户/公开/后台视图转换      |
| `SysUserFollowMapper.java`     | `mapper/`                     | 关系查询、公开查询、后台查询与清理 |
| `SysUserFollowMapper.xml`      | `resources/.../mapper/`       | follow 相关联表 SQL   |
| `follow-api.md`                | `docs/api文档/`                 | 用户/公开/后台接口文档      |

## 6. 下一阶段建议

1. **优先保持主页解耦**，如果后续首页/个人主页真的需要关注维度展示，再按明确场景补关系读取，不要现在提前耦合。
2. **Redis 缓存先继续观察**，等关注统计或公开列表出现明确热点，再决定是否引入缓存和失效策略。
3. **继续补更真实的集成验证**，重点放在通知投递幂等、后台清理脚本配合和前后端联调稳定性。
4. **保持 follow 独立语义**，不要回退去复用 `sys_interaction`。

## 7. 完成标志

- 用户侧关注/取关/列表/互关/统计主链路可联调。
- 匿名访客可查看公开关注/粉丝列表。
- 后台可分页排查并清理异常关注关系。
- 新粉丝通知在主事务提交后补写，不影响关注主链路成功。
- 主页联动边界和缓存策略结论清晰，不需要再回头重做基础表结构与接口设计。
