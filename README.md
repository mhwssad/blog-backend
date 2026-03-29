# blog-backend

`blog-backend` 是一个基于 Spring Boot 4、Spring Security、MyBatis-Plus、MySQL、Redis 的博客后端项目，当前仓库重点已经落地认证鉴权、RBAC 后台管理、通知中心、内容域接口以及文件上传管理能力。

文档入口优先看 [`docs/README.md`](docs/README.md)。

## 项目情况

- 技术栈：Java 17、Spring Boot 4.0.3、Spring Security、MyBatis-Plus、Druid、Redis、Knife4j/OpenAPI、MapStruct、Lombok。
- 当前默认环境：`dev`，启动端口 `8000`。
- 当前已落地的业务主线：
  - 认证与会话：账号登录、注册、邮箱验证码登录、刷新令牌、退出登录。
  - RBAC 管理：用户、角色、菜单、系统配置、通知、日志后台接口。
  - 内容域：文章、分类、标签、评论、收藏、互动、足迹接口。
  - 文件域：用户上传、秒传/分片上传、文件后台管理。
- 当前阶段重点：
  - 继续整理公共能力、测试支撑、文档入口和历史结构问题。
  - 聊天 / WebSocket 当前仅保留数据库与单机握手骨架，待主线能力继续稳定后再进入正式业务实现。

## 架构概览

项目目前是单模块单体后端，按“配置层 -> Web 层 -> 业务层 -> 数据访问层 -> 数据库/缓存”组织：

```text
src/main/java/com/cybzacg/blogbackend
├─ config           Spring 与中间件配置
├─ core             统一过滤器、安全能力、统一响应
├─ common           常量、Redis 工具
├─ exception        业务异常与统一异常处理
├─ domain           数据库实体
├─ mapper           MyBatis-Plus Mapper 接口
├─ module
│  ├─ auth          认证、RBAC、通知中心
│  ├─ article       文章内容域
│  ├─ content       分类/标签/评论/收藏/互动/足迹
│  ├─ file          文件上传与后台管理
│  ├─ chat          聊天会话、消息与实时推送
│  └─ follow        关注关系预留骨架
└─ utils            通用工具类
```

核心运行链路：

1. HTTP 请求先经过 `TokenAuthenticationFilter` 做令牌解析。
2. Spring Security 按 `SecurityConfig` 中的白名单与鉴权规则决定是否放行。
3. Controller 返回统一的 `Result` / `PageResult`。
4. Service 负责业务编排、权限校验、令牌管理、通知处理等逻辑。
5. Mapper + XML 负责数据库访问，底层数据源默认连接 MySQL，缓存与令牌可接 Redis。

配置与资源文件：

- `src/main/resources/application.yml`：基础配置，默认激活 `dev`。
- `src/main/resources/application-dev.yml`：本地开发环境配置。
- `src/main/resources/application-prod.yml`：生产环境示例配置。
- `src/main/resources/mysql/1.sys.sql`：系统基础表结构。
- `src/main/resources/mysql/02_article.sql`：文章内容域表结构。
- `src/main/resources/mysql/03_permission_init.sql`：权限与菜单初始化脚本。
- `src/main/resources/mysql/04_file.sql`：文件域表结构。
- `src/main/resources/mysql/05_chat.sql`：聊天域表结构（单聊 / 群聊 / 全站群、消息、接收状态、已读游标）。
- `src/main/resources/mysql/06_follow.sql`：粉丝关注关系表结构（关注、取关、粉丝列表、关注列表、互关判断）。
- `src/main/resources/mysql/07_schema_repair.sql`：存量库修复脚本，用于补齐缺失表并修复系统表唯一约束、索引和引擎问题。
- `src/main/java/com/cybzacg/blogbackend/config/WebSocketConfig.java`：WebSocket 入口配置，当前默认端点为 `/ws/chat`。
- `docs/chat-implementation.md`：聊天模块实现说明。
- `docs/api文档/chat-api.md`：聊天 HTTP 接口与 WebSocket 协议文档。

## 本地依赖

建议本地至少准备以下环境：

- JDK 17
- Maven 3.9+，或者直接使用仓库自带的 `mvnw.cmd`
- MySQL 8.x
- Redis 6.x/7.x

默认 `dev` 配置使用：

- MySQL：`localhost:3306/blog_backend`
- Redis：`localhost:6379`，数据库 `12`
- 应用端口：`8000`
- WebSocket：`ws://localhost:8000/ws/chat?accessToken=<accessToken>`

首次启动前，至少要完成：

1. 创建数据库 `blog_backend`。
2. 空库按顺序执行 `1.sys.sql`、`02_article.sql`、`04_file.sql`、`05_chat.sql`、`06_follow.sql`、`03_permission_init.sql`。
3. 如果是历史库升级，不要直接把 `04_file.sql`、`05_chat.sql`、`06_follow.sql` 当增量脚本执行；应改为执行 `07_schema_repair.sql` 做存量库修复。
4. 根据本机环境修改 `application-dev.yml` 中的数据库、Redis、邮件配置。

## 怎么样编译

Windows 下优先直接用 Maven Wrapper：

```powershell
.\mvnw.cmd -q -DskipTests compile
```

如果你本机已经装了 Maven，也可以：

```powershell
mvn -q -DskipTests compile
```

常见补充命令：

```powershell
.\mvnw.cmd clean package -DskipTests
.\mvnw.cmd spring-boot:run
```

说明：

- 首次编译需要联网下载依赖。
- 当前仓库已经可以通过 `compile` 阶段。
- 如果只是本地调试接口，推荐先确认 MySQL 和 Redis 都可连通后再执行 `spring-boot:run`。

## 怎么样测试

当前仓库已经不止一个测试文件，现有测试覆盖：

- Spring 上下文加载测试：`src/test/java/com/cybzacg/blogbackend/BlogBackendApplicationTests.java`
- `auth` 模块服务级与后台权限 WebMvc 测试
- `article` 模块控制器与访问控制测试
- `content` 模块安全与服务测试
- `file` 模块控制器、权限与服务测试（已覆盖秒传收口、普通上传、分片校验、后台删除与权限边界）

运行命令：

```powershell
.\mvnw.cmd test
```

或者：

```powershell
mvn test
```

测试注意点：

- `BlogBackendApplicationTests` 已切换到独立 `test` profile。
- `test` profile 使用 H2 内存数据库、本地测试存储目录，并排除了会在启动期强依赖 Redis 的 Redisson 自动配置。
- 这意味着基础上下文测试不再依赖开发环境 MySQL/Redis 才能启动。
- 当前其余测试以轻量单测、隔离 WebMvc 与权限测试为主，已能在不依赖开发环境 MySQL/Redis 的前提下覆盖更多高风险链路。

如果只是想先做一轮基础自检，建议顺序是：

1. `.\mvnw.cmd -q -DskipTests compile`
2. `.\mvnw.cmd -Dtest=BlogBackendApplicationTests test`
3. `.\mvnw.cmd spring-boot:run`
4. 打开 `http://localhost:8000/doc.html` 或 `http://localhost:8000/swagger-ui.html`
5. 视改动范围再执行 `.\mvnw.cmd test`

## 接口与文档

- 文档总导航：`docs/README.md`
- 认证与系统管理接口说明：`docs/api文档/auth-api.md`
- 内容域接口说明：`docs/api文档/content-api.md`
- 文件模块接口说明：`docs/api文档/file-api.md`
- 聊天 / WebSocket 预留说明：`docs/api文档/chat-api.md`
- 项目进度文档：`docs/项目进度文档.md`
- 后续更新计划：`docs/后续更新计划.md`
- 数据库修复说明：`docs/db-schema-repair.md`
- 内容域任务拆分：`docs/tasks/content-domain/`
- Swagger / Knife4j：
  - `http://localhost:8000/doc.html`
  - `http://localhost:8000/swagger-ui.html`

## 当前维护建议

- 优先复用 `docs/README.md` 中的正式入口文档，不再在 README 外散落平行说明。
- 继续把更多真正依赖 Spring 上下文的测试接入 `test` profile，并优先抽公共测试支撑，减少重复样板代码。
- 在 `P2` 阶段保持“先整理公共能力与历史遗留，再进入聊天 / WebSocket 业务实现”的推进节奏。





