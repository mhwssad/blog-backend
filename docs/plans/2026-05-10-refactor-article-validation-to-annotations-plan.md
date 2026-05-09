---
title: "refactor(article): 将服务层字段验证迁移到 Bean Validation 注解"
type: refactor
status: active
date: 2026-05-10
deepened: 2026-05-10
---

# refactor(article): 将服务层字段验证迁移到 Bean Validation 注解

## Enhancement Summary

**Deepened on:** 2026-05-10
**Research agents used:** Bean Validation 最佳实践、项目现有模式分析、ArticleStatusMachine 集成分析、架构策略审查、模式识别审查

### Key Improvements from Deepening

1. **放弃 3 个自定义验证注解**（`@UniqueElements`、`@ValidArticleSaveRequest`、`@UniqueAccessItems`）—— 架构审查和模式识别审查一致认为：这些跨字段/跨元素的校验与 DB 查询紧密交织，强行拆分到注解层会破坏验证逻辑的内聚性，且 Bean Validation 无法返回转换后的值（如去重列表）
2. **聚焦简单字段级注解**—— 只迁移纯结构性校验（非空、长度、范围、枚举），这些与 `core/validation/` 现有模式完全一致
3. **新增 `@Valid` 级联验证**—— 在 `accessList` 字段上添加 `@Valid` 以触发嵌套 `ArticleAccessItem` 的字段级注解
4. **接受双重验证**—— 枚举/范围字段会被注解和 `ArticleStatusMachine` 各校验一次，作为已知权衡（注解引用同一枚举类，自动保持同步）

### Research Insights

**Bean Validation 最佳实践：**
- 类级验证器应使用 `addPropertyNode()` 将错误绑定到具体字段，方便前端展示
- `ConstraintValidator` 实例被缓存复用，`isValid()` 不能存储状态
- 调用 `disableDefaultConstraintViolation()` 后必须再调用 `addConstraintViolation()`
- 所有新验证器都可以是纯函数（不需要 Spring DI）

**项目模式约定：**
- `core/validation/` 只放置纯结构性、无依赖的验证注解
- 可选字段验证器 null/空返回 true（由 `@NotNull`/`@NotBlank` 控制必填）
- 验证错误统一由 `ValidationExceptionHandler` 处理（错误码 40001，HTTP 400）

**ArticleStatusMachine 集成分析：**
- 所有 `normalize*()` 方法同时做归一化（null→默认值）和验证（范围检查），无法拆分
- `validateSaveState()` 是完全混合的：归一化 + 验证 + 跨字段业务逻辑
- `categoryIds`/`tagIds` 的去重结果被后续 `syncCategoryBindings`/`syncTagBindings` 使用

---

## Overview

将 `module/article` 目录下**纯结构性字段验证**从 Service 层迁移到 JSR-303/Jakarta Bean Validation 注解。跨字段条件校验、列表元素校验和 DB 存在性校验保留在 Service 层。

## Problem Statement / Motivation

当前 article 模块存在两类可改善的验证问题：

1. **冗余校验**：`ArticleSeriesServiceImpl` 中 `title` 非空检查与 DTO 上的 `@NotBlank` 重复；`ArticleAdminCrudServiceImpl` 中 `authorId` null 检查与 `@NotNull` 重复
2. **缺失注解**：多个 DTO 字段（如 `status`、`accessLevel`、`visibilityScope`、`ArticleAccessItem.userId`）缺少任何 Bean Validation 注解，验证完全依赖 Service 层

## Proposed Solution

| 验证类型 | 处理方式 | 示例 |
|---|---|---|
| 简单字段（非空、长度、范围、枚举） | 标准 + 项目已有注解 | `@NotNull`, `@Size`, `@EnumValue`, `@Min`, `@Max` |
| 嵌套对象字段 | `@Valid` 级联验证 | `accessList` 字段上的 `@Valid` |
| 跨字段条件校验 | **保留在 Service 层** | `isOriginal==0` → `sourceUrl` 必填 |
| 列表元素校验（去重） | **保留在 Service 层** | `IdCollectionUtils.requireUniqueNonNullIds()` |
| DB 存在性校验 | **保留在 Service 层** | 分类/标签/用户存在性 |

### Why NOT Create Custom Validators?

在深化研究中，架构审查和模式识别审查一致建议**不要创建** `@UniqueElements`、`@ValidArticleSaveRequest`、`@UniqueAccessItems` 这三个自定义注解。核心理由：

1. **Bean Validation 无法返回转换后的值**：`IdCollectionUtils.requireUniqueNonNullIds()` 同时验证和返回去重列表，注解只能验证不能转换。拆分后 Service 层仍需调用去重方法，注解只提供早期拦截，ROI 为负。

2. **跨字段校验与 DB 查询紧密耦合**：`validateSaveRequest()` 中的跨字段规则（`isOriginal`→`sourceUrl`、WHITELIST→`accessList`）与同一方法内的作者存在性、分类存在性、配额检查等 DB 校验交织在一起。将部分逻辑提取到注解会打破验证的内聚性。

3. **`core/validation/` 应只放纯结构性校验**：现有 5 个自定义注解都是无依赖、无领域语义的结构性检查。引入需要理解 `ArticleAccessItem` 语义或需要调用 Repository 的注解会破坏这一清晰边界。

## Technical Approach

### Phase 1: DTO 注解补充（纯结构性字段）

#### 1.1 `ArticleSaveRequest`（admin）

| 字段 | 现有注解 | 新增注解 |
|---|---|---|
| `status` | 无 | `@EnumValue(enumClass = ArticleStatusEnum.class, message = "文章状态值无效")`（注：需确认是否存在 `ArticleStatusEnum`，否则用 `@Min(0) @Max(2)`） |
| `isOriginal` | 无 | `@Min(value = 0, message = "原创标识必须为 0 或 1") @Max(value = 1, message = "原创标识必须为 0 或 1")` |
| `isTop` | 无 | `@Min(value = 0, message = "置顶标识必须为 0 或 1") @Max(value = 1, message = "置顶标识必须为 0 或 1")` |
| `isRecommend` | 无 | `@Min(value = 0, message = "推荐标识必须为 0 或 1") @Max(value = 1, message = "推荐标识必须为 0 或 1")` |
| `accessLevel` | 无 | `@Min(value = 0, message = "访问级别必须在 0-4 之间") @Max(value = 4, message = "访问级别必须在 0-4 之间")` |
| `visibilityScope` | 无 | `@EnumValue(enumClass = ArticleVisibilityScopeEnum.class, message = "可见范围值无效")` |
| `accessList` | 无 | `@Valid`（级联触发 `ArticleAccessItem` 校验） |

**注意**：`categoryIds` 和 `tagIds` 的去重校验保留在 Service 层（`IdCollectionUtils.requireUniqueNonNullIds`），不添加 `@UniqueElements`。

**注意**：不添加类级 `@ValidArticleSaveRequest`。跨字段规则（`isOriginal==0`→`sourceUrl`、WHITELIST→`accessList`）保留在 `validateSaveRequest()` 中。

#### 1.2 `ArticleAccessItem`（admin）

| 字段 | 新增注解 |
|---|---|
| `userId` | `@NotNull(message = "授权用户不能为空")` |
| `accessType` | `@Min(value = 1, message = "访问类型必须为 1 或 2") @Max(value = 2, message = "访问类型必须为 1 或 2")`（null 合法，默认由 Service 层补为 1） |
| `grantReason` | `@Size(max = 256, message = "授权原因最多256个字符")` |

#### 1.3 `ArticleAccessAssignRequest`（admin）

| 字段 | 新增注解 |
|---|---|
| `accessList` | `@Valid`（级联触发 `ArticleAccessItem` 的 `@NotNull`/`@Min`/`@Max` 校验） |

**注意**：不添加 `@UniqueAccessItems`。重复 `userId:accessType` 检查保留在 `validateAccessItems()` 中。

#### 1.4 `ArticleStatusRequest`（admin）

| 字段 | 现有注解 | 新增注解 |
|---|---|---|
| `status` | `@NotNull` | `@Min(value = 0, message = "文章状态必须在 0-2 之间") @Max(value = 2, message = "文章状态必须在 0-2 之间")` |

#### 1.5 `ArticleReviewRepairRequest`（admin）

| 字段 | 现有注解 | 新增注解 |
|---|---|---|
| `targetReviewStatus` | `@NotNull` | `@EnumValue(enumClass = ArticleReviewStatusEnum.class, message = "目标审核状态非法")` |
| `reviewComment` | `@Size(max=512)` | 追加 `@NotBlank(message = "修正说明不能为空")` |

#### 1.6 `ArticleSeriesSaveRequest`（user）

| 字段 | 新增注解 |
|---|---|
| `status` | `@Min(value = 0, message = "系列状态必须为 0 或 1") @Max(value = 1, message = "系列状态必须为 0 或 1")` |
| `sortOrder` | `@Min(value = 0, message = "排序值不能为负数")` |

### Phase 2: Service 层清理

#### 2.1 `ArticleAdminCrudServiceImpl`

**移除的校验：**

- `validateAuthor()` 中移除 `authorId` null 检查 → 已有 `@NotNull`（冗余）

**保留的校验（全部保留）：**

- `validateSaveRequest()` 整体保留 —— 包含跨字段规则（`isOriginal`→`sourceUrl`、WHITELIST→`accessList`）、`IdCollectionUtils` 去重、DB 存在性检查
- `validateAuthor()` — 用户存在性 + 未删除检查
- `validateCategories()` — 分类存在性检查
- `validateTags()` — 标签存在性检查
- `validateUpdateAllowed()` — 审核中禁止编辑
- `validateAuthorArticleQuota()` — 配额检查
- `articleStatusMachine.validateSaveState()` — 状态机约束

**Research Insight — 双重验证说明：**

枚举/范围字段（`status`、`visibilityScope`、`accessLevel` 等）会被注解和 `ArticleStatusMachine` 各校验一次。这是已知权衡：
- 注解引用同一枚举类（如 `ArticleVisibilityScopeEnum.class`），自动与枚举定义保持同步
- `ArticleStatusMachine` 的 `normalize*()` 方法额外做了归一化（null→默认值），这是注解无法替代的
- 如果枚举新增值，`@EnumValue` 自动识别，但 `ArticleStatusMachine` 中的硬编码常量需手动更新——因此注解实际上是更可靠的单一真相来源

#### 2.2 `ArticleAccessManageServiceImpl`

**移除的校验：**

- `validateAccessItems()` 中移除：
  - `item.getUserId()` null 检查 → `@NotNull` on `ArticleAccessItem.userId`
  - `accessType` 范围检查 → `@Min/@Max` on `ArticleAccessItem.accessType`

**保留的校验：**

- 重复 `userId:accessType` 检查 → 保留（需要遍历列表收集已见 key，属于跨元素逻辑）
- 用户批量存在性检查 → 保留（DB 查询）

#### 2.3 `ArticleSeriesServiceImpl`

**移除的校验：**

- `validateSeriesSaveRequest()` 中移除：
  - `title` 非空检查 → 已有 `@NotBlank`（冗余）
  - `status` 范围检查 → `@Min(0) @Max(1)` on DTO

**保留的校验：**

- `normalizeSeriesStatus()` 的归一化逻辑（null→1）→ 保留（`@Min/@Max` 不做归一化）
- `normalizeSeriesVisibilityScope()` → 整体保留（归一化 + WHITELIST 不支持的业务约束）
- 标题唯一性检查 → 保留（DB 查询）

#### 2.4 `ArticleReviewAdminServiceImpl`

**移除的校验：**

- `repairReviewStatus()` 中移除：
  - `reviewComment` 非空检查 → `@NotBlank` on DTO
  - `targetReviewStatus` 枚举检查 → `@EnumValue` on DTO
  - `request == null` 检查 → Spring `@RequestBody` 已保证

**保留的校验：**

- `rejectReview()` 中 `reviewComment` 非空检查 → **保留**（上下文相关：仅在拒绝时必填，批准时可选。`ArticleReviewDecisionRequest.reviewComment` 不能加 `@NotBlank`）
- 目标状态与当前状态比较 → 保留（跨 DB 状态校验）
- 文章存在性 + 当前审核状态校验 → 保留

### Phase 3: 编译验证 + 测试

- 编译：`./mvnw.cmd -q -DskipTests compile`
- 验证 `@Valid` 级联触发 `ArticleAccessItem` 的 `@NotNull`/`@Min`/`@Max`
- 验证 `@EnumValue` 对 `ArticleReviewStatusEnum`（`getValue()`）和 `ArticleVisibilityScopeEnum`（`getValue()`）正常工作
- 检查已有单元测试是否因验证位置变更而失败
- 运行测试：`./mvnw.cmd test`

## Acceptance Criteria

### Phase 1: DTO 注解

- [ ] `ArticleSaveRequest` 补充枚举/范围/级联注解（不创建类级注解）
- [ ] `ArticleAccessItem` 补充 `@NotNull` + `@Min/@Max` + `@Size`
- [ ] `ArticleAccessAssignRequest` 补充 `@Valid`（不创建 `@UniqueAccessItems`）
- [ ] `ArticleStatusRequest` 补充 `@Min(0) @Max(2)`
- [ ] `ArticleReviewRepairRequest` 补充 `@NotBlank` + `@EnumValue`
- [ ] `ArticleSeriesSaveRequest` 补充 `@Min(0) @Max(1)` + `@Min(0)`

### Phase 2: Service 清理

- [ ] `ArticleAdminCrudServiceImpl.validateAuthor()` 移除冗余 `authorId` null 检查
- [ ] `ArticleAccessManageServiceImpl.validateAccessItems()` 移除 `userId` null 和 `accessType` 范围检查
- [ ] `ArticleSeriesServiceImpl.validateSeriesSaveRequest()` 移除冗余 `title` 非空检查和 `status` 范围检查
- [ ] `ArticleReviewAdminServiceImpl.repairReviewStatus()` 移除 `reviewComment`/`targetReviewStatus`/`request==null` 检查

### Phase 3: 验证

- [ ] 编译通过
- [ ] 已有测试通过

## Dependencies & Risks

**依赖：**
- 项目已有的 `@EnumValue` + `EnumValueValidator`（已支持 `method` 属性，默认 `getValue`）
- Article 模块的枚举都使用 `getValue()` 方法（`ArticleReviewStatusEnum`、`ArticleVisibilityScopeEnum`），兼容 `@EnumValue` 默认配置
- 所有 Controller 的 `@RequestBody` 参数已有 `@Valid`

**风险与权衡：**

| 风险 | 影响 | 缓解 |
|---|---|---|
| 枚举字段双重验证 | `@EnumValue` 和 `ArticleStatusMachine.normalize*()` 都会验证 | 可接受：注解引用同一枚举类自动同步；`normalize*()` 额外做归一化 |
| `categoryIds`/`tagIds` 去重保留在 Service | 未完全满足"不在 Service 层验证"的要求 | `IdCollectionUtils` 同时负责验证和数据准备，Bean Validation 无法替代数据转换 |
| 跨字段校验保留在 Service | `isOriginal`→`sourceUrl`、WHITELIST→`accessList` 未声明化 | 这些规则与 DB 存在性检查交织，保持内聚性优于强行拆分 |
| `rejectReview()` 的条件校验 | `reviewComment` 仅拒绝时必填 | 正确的架构选择：业务动作决定字段必填性，DTO 注解无法感知调用上下文 |

## File Change List

### 新建文件

**无。** 深化研究后决定不创建自定义验证注解。

### 修改文件

| 文件 | 变更说明 |
|---|---|
| `module/article/model/admin/ArticleSaveRequest.java` | 补充 `@EnumValue`/`@Min/@Max`/`@Valid` 注解 |
| `module/article/model/admin/ArticleAccessItem.java` | 补充 `@NotNull` / `@Min/@Max` / `@Size` |
| `module/article/model/admin/ArticleAccessAssignRequest.java` | 补充 `@Valid` |
| `module/article/model/admin/ArticleStatusRequest.java` | 补充 `@Min(0) @Max(2)` |
| `module/article/model/admin/ArticleReviewRepairRequest.java` | 补充 `@NotBlank` + `@EnumValue` |
| `module/article/model/user/ArticleSeriesSaveRequest.java` | 补充 `@Min(0) @Max(1)` + `@Min(0)` |
| `module/article/service/impl/ArticleAdminCrudServiceImpl.java` | 移除 `validateAuthor()` 中冗余 `authorId` null 检查 |
| `module/article/service/impl/ArticleAccessManageServiceImpl.java` | 移除 `validateAccessItems()` 中 `userId`/`accessType` 校验 |
| `module/article/service/impl/ArticleSeriesServiceImpl.java` | 移除冗余 `title`/`status` 校验 |
| `module/article/service/impl/ArticleReviewAdminServiceImpl.java` | 移除 `repairReviewStatus()` 中已迁移的校验 |

## Sources & References

- 项目已有自定义注解：`core/validation/EnumValue.java`（支持 `method` 属性）、`core/validation/ValidJsonObject.java`
- 文章状态机：`module/article/service/ArticleStatusMachine.java`
- ID 列表工具：`utils/IdCollectionUtils.java`
- 枚举：`enums/article/ArticleReviewStatusEnum.java`（`getValue()`）、`enums/article/ArticleVisibilityScopeEnum.java`（`getValue()`）
- 验证异常处理：`exception/handler/ValidationExceptionHandler.java`（错误码 40001）
- 编码规范：`docs/项目代码编写规范.md` 第 2.1 节（"参数校验优先使用 Bean Validation 注解"）
- Hibernate Validator 9.0.1.Final Reference Guide：类级验证器、级联验证、容器元素约束
