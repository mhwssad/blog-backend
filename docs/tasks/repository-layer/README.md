# Repository 层重构计划

## 概述

当前项目采用 Controller → Service(继承IService) → Mapper 三层架构。Service 层存在两种角色：

- **30个"薄数据服务"**：空壳继承 `IService<PO>` / `ServiceImpl<Mapper, PO>`，本质是数据访问层
- **32个"业务服务"**：包含业务逻辑，但直接调用 `lambdaQuery()`、`LambdaQueryWrapper`、Mapper 等数据操作

**目标**：引入 Repository 层，将所有数据操作从 Service 剥离，使 Service 专注于业务逻辑。

## 架构目标

```
Controller ──→ Service（业务逻辑）──→ Repository（数据访问）──→ Mapper
                     │                        │
                     │ 禁止直接操作Mapper        │ 继承 IService<PO>
                     │ 禁止构建查询条件           │ LambdaQueryWrapper链式查询
                     │ 禁止继承IService          │ 包装XML多表SQL
```

## 包结构

每个模块新增 `repository/` 和 `repository/impl/` 子包：

```
module/{模块}/
├── controller/
├── convert/
├── model/
├── service/impl/          ← 业务服务（保留）
├── repository/            ← 【新增】Repository 接口
│   └── impl/              ← 【新增】Repository 实现
```

## 命名规范

| 角色            | 命名                                  | 示例                                           |
| --------------- | ------------------------------------- | ---------------------------------------------- |
| Repository 接口 | `{Entity}Repository`                  | `SysUserFollowRepository`                      |
| Repository 实现 | `{Entity}RepositoryImpl`              | `SysUserFollowRepositoryImpl`                  |
| 查询方法        | `findByXxx` / `listByXxx` / `pageXxx` | `findByFollowerAndFollowing(userId, targetId)` |
| 判断方法        | `existsByXxx`                         | `existsActiveRelation(userId, targetId)`       |
| 统计方法        | `countByXxx`                          | `countActiveFollowing(userId)`                 |
| 删除方法        | `removeByXxx`                         | `removeByArticleId(articleId)`                 |
| 更新方法        | `updateXxxToYyy`                      | `updateDeliveryToDelivered(...)`               |
| 包装Mapper XML  | 与Mapper方法同名                      | `selectFollowPage(...)`                        |

## 迁移策略（双阶段并行法）

### Phase A — 创建 Repository（与旧服务并行共存）

1. 创建 `{Entity}Repository` 接口（extends `IService<PO>`）
2. 创建 `{Entity}RepositoryImpl`（extends `ServiceImpl<Mapper, PO>`）
3. 复制薄数据服务中的自定义方法到 Repository
4. 旧薄数据服务保持不变

### Phase B — 迁移业务服务

1. 将注入的 Tier-1 服务类型替换为 Repository 类型
2. 提取所有 `lambdaQuery()` / `lambdaUpdate()` / `LambdaQueryWrapper` / 直接 Mapper 调用到 Repository 的命名方法
3. 业务服务仅调用 Repository 方法
4. 模块内所有业务服务迁移完成后，删除旧 Tier-1 服务

**关键原则**：任何时刻项目可编译、测试可通过。

## 执行顺序

建议按依赖关系分6轮执行，但每轮内的模块计划是自包含的，可并行开发：

| 轮次 | 模块    | 计划文件                                     | 依赖                | 复杂度         |
| ---- | ------- | -------------------------------------------- | ------------------- | -------------- |
| 1    | follow  | [01-follow-module.md](01-follow-module.md)   | 无                  | 低（原型验证） |
| 2    | content | [02-content-module.md](02-content-module.md) | 无                  | 中             |
| 3    | auth    | [03-auth-module.md](03-auth-module.md)       | 无                  | 中             |
| 4    | file    | [04-file-module.md](04-file-module.md)       | 无                  | 中             |
| 5    | article | [05-article-module.md](05-article-module.md) | content, auth, file | 高             |
| 6    | chat    | [06-chat-module.md](06-chat-module.md)       | auth, file          | 高             |

**并行说明**：

- follow / content / auth / file 可同时并行（互不依赖）
- article 需等 content + auth + file 完成后再迁移
- chat 需等 auth + file 完成后再迁移

## 注意事项

1. **@Transactional 保留在 Service 层**：98个 `@Transactional` 全部留在业务服务上
2. **唯一例外**：`SysLogServiceImpl` 的 `REQUIRES_NEW` 传播行为随 Repository 迁移
3. **DuplicateKeyException**：Repository 抛出，Service 层捕获（业务决策）
4. **跨模块注入**：业务服务可注入其他模块的 Repository 接口（公共 Spring Bean）

## 验证方式

每轮迁移完成后：

1. `mvn compile -q` 无错误
2. `mvn test` 全部通过
3. 确认业务服务中无 `lambdaQuery()`、`lambdaUpdate()`、`new LambdaQueryWrapper<>()`、直接 Mapper 注入
