# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Compile (quick check)
./mvnw.cmd -q -DskipTests compile

# Run all tests
./mvnw.cmd test

# Run a single test class
./mvnw.cmd -Dtest=PublicForumServiceImplTest test

# Run a single test method
./mvnw.cmd -Dtest=PublicForumServiceImplTest#getPostShouldRejectLoginOnlyPostForAnonymous test

# Start the application (requires MySQL + Redis)
./mvnw.cmd spring-boot:run

# Package
./mvnw.cmd clean package -DskipTests
```

## Project Overview

Java 17 + Spring Boot 3.5.3 单体博客后端。技术栈：Spring Security、MyBatis-Plus、MySQL、Redis (Redisson)
、Druid、Knife4j/OpenAPI、MapStruct、Lombok、WebSocket。

默认 dev 环境：MySQL `localhost:3306/blog_backend`、Redis `localhost:6379` 数据库 12、应用端口 `8000`。

当前项目已进入第二期：论坛 P0 已进入待联调；下一步按 `docs/tasks/README.md` 推荐顺序推进 AI 知识库 / RAG / agents 的 P0 知识源边界。第一期任务清单已归档，当前任务入口统一以 `docs/tasks/README.md` 为准。

## Architecture

```
src/main/java/com/cybzacg/blogbackend
├─ config           Spring 与中间件配置
├─ core             统一过滤器、安全能力、统一响应 (Result / PageResult)
├─ common           常量、Redis 工具 (RedisOperator)、存储抽象、切面
├─ exception        BusinessException + ResultCode，全局异常处理器
├─ domain           数据库实体 (MyBatis-Plus)
├─ enums            跨模块枚举、结果码、状态码
├─ mapper           MyBatis-Plus Mapper 接口 + resources/mapper/*.xml
├─ module           全部业务域实现
│  ├─ auth          认证、RBAC、作者申请、通知中心、经验体系、超级管理员
│  ├─ article       文章内容域
│  ├─ content       分类/标签/评论/收藏/互动/足迹
│  ├─ file          文件上传与后台管理 (秒传/分片)
│  ├─ chat          聊天会话、频道、群组与 WebSocket 推送
│  ├─ follow        用户关注关系
│  ├─ ai            AI 对话、渠道、额度与统计
│  ├─ report        举报处理与治理联动
│  ├─ dashboard     后台数据看板
│  └─ forum         论坛版块、帖子、回复与频道挂接
└─ utils            纯工具类
```

### 请求链路

HTTP → `TokenAuthenticationFilter` → Spring Security (`SecurityConfig` 白名单/鉴权) → Controller → Service →
Repository → Mapper → MySQL

### 业务模块标准结构

```
module/<domain>
├─ controller      接口入口 (按 UserXxxController / XxxAdminController / PublicXxxController 拆分)
├─ service/impl    业务编排、事务边界、权限校验
├─ model           请求/响应模型 (admin/ | user/ | publics/ | common/ | data/)
├─ convert         MapStruct 映射器 (XxxModelMapper)
├─ repository/impl 数据访问组织 (继承 IService/ServiceImpl)
├─ constant        模块私有常量
├─ config          模块私有配置
├─ task            模块专属调度任务
└─ websocket       WebSocket 协议处理
```

### 依赖方向

`controller → service → repository → mapper → database`。禁止反向依赖，Controller 不直接依赖 Repository/Mapper。

## Key Conventions

- **规范入口**：所有开发、重构、接口调整、文档更新和提交必须遵循 `AGENTS.md`、`docs/项目代码编写规范.md`、`docs/项目结构规范.md`。
- **实体转换**：优先 MapStruct，其次 `BeanConverterUtils`，禁止大段手写 `new Entity() + setXxx()`。
- **异常处理**：统一 `BusinessException` + `ResultCode`，Service 层通过 `ExceptionThrowerCore` 抛出，不要手动捕获业务异常后重新包装。
- **Redis**：统一通过 `RedisOperator` 操作，Key 用 `RedisKeyUtils.build()` 拼接，Key 前缀定义为常量，所有 Key 必须设 TTL。
- **日志**：`@Slf4j` 注解，不在正常 CRUD 中加冗余日志，日志不含敏感信息。
- **注释**：不易理解的方法必须补 Javadoc，即使是私有 helper；注释写职责和约束，不逐行复述代码。
- **当前数据库变更口径**：开发阶段默认直接维护原始建表脚本；只有用户明确要求时，才单独补迁移脚本。

## Testing

- 框架：JUnit 5 + Mockito，`@ExtendWith(MockitoExtension.class)`。
- 测试使用 `test` profile，H2 内存数据库，排除 Redisson 自动配置，不依赖外部 MySQL/Redis。
- 测试类命名 `{被测类名}Test`，与生产代码模块路径对齐。
- Service 测试 mock Repository 和外部 Service，不 mock 被测服务自身内部方法。
- 新增或修改 Service 方法时必须补充测试，覆盖正常路径、边界条件和异常分支。
- 论坛 P0 已有 `PublicForumServiceImplTest`、`UserForumServiceImplTest`、`ForumPostChannelLinkServiceImplTest`；后续论坛后台治理需继续补后台权限测试。

## Git Conventions

- 提交格式：`type(scope): 简要说明`，如 `feat(file): 新增分片上传完成接口`。
- scope 取模块名：`auth`、`article`、`content`、`file`、`chat`、`follow`、`docs`、`api`。
- 一次提交只解决一类问题，禁止混合功能开发、重构、文档更新。
- 代码必须可编译后再提交，非平凡改动至少执行一次 `mvn -q -DskipTests compile`。
- **分步提交**：每完成一个逻辑步骤（如基础设施层、数据访问层、服务层、接口层）后立即提交，保持提交粒度小且可追溯。不要等整个功能全部完成后才一次性提交。
- 当前仓库可能存在用户未提交改动；提交前必须检查 `git status --short`，只 stage 当前任务相关文件，不要回滚或混入无关改动。

## Documentation

新增/修改接口时必须同步更新 `docs/api文档` 中对应文档。影响项目进度或计划时同步更新 `docs/tasks/README.md`
或 `docs/tasks` 下对应任务清单。

当前主要文档入口：

- `docs/tasks/README.md`：第二期任务导航与推荐执行顺序。
- `docs/tasks/01-forum-module-todo.md`：论坛 P0/P1 任务状态。
- `docs/api文档/forum-api.md`：论坛公开侧、用户侧和帖子频道挂接接口。
- `docs/api文档/ai-api.md`：AI 当前接口，后续 RAG / agents 扩展也应同步更新。

完整规范详见 [AGENTS.md](AGENTS.md)、[docs/项目代码编写规范.md](docs/项目代码编写规范.md)、[docs/项目结构规范.md](docs/项目结构规范.md)。
