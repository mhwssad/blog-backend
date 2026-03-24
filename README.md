# blog-backend

`blog-backend` 是一个基于 Spring Boot 4、Spring Security、MyBatis-Plus、MySQL、Redis 的博客后端项目，当前仓库重点已经落地认证鉴权、RBAC 后台管理、通知中心、内容域接口以及文件上传管理能力。

## 项目情况

- 技术栈：Java 17、Spring Boot 4.0.3、Spring Security、MyBatis-Plus、Druid、Redis、Knife4j/OpenAPI、MapStruct、Lombok。
- 当前默认环境：`dev`，启动端口 `8000`。
- 当前已落地的业务主线：
  - 认证与会话：账号登录、注册、邮箱验证码登录、刷新令牌、退出登录。
  - RBAC 管理：用户、角色、菜单、系统配置、通知、日志后台接口。
  - 内容域：文章、分类、标签、评论、收藏、互动、足迹接口。
  - 文件域：用户上传、秒传/分片上传、文件后台管理。
- 当前在建部分：
  - 继续补齐测试、文档和边界场景验证。

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
│  └─ file          文件上传与后台管理
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

首次启动前，至少要完成：

1. 创建数据库 `blog_backend`。
2. 按顺序执行 `1.sys.sql`、`02_article.sql`、`03_permission_init.sql`。
3. 根据本机环境修改 `application-dev.yml` 中的数据库、Redis、邮件配置。

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
- `article` 模块控制器与访问控制测试
- `content` 模块安全与服务测试
- `file` 模块控制器与服务测试（已覆盖秒传收口、引用删除）

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
- 当前其余测试仍以轻量单测和 MockMvc 为主，后续还需要继续扩大对高风险链路的自动化覆盖。

如果只是想先做一轮基础自检，建议顺序是：

1. `.\mvnw.cmd -q -DskipTests compile`
2. `.\mvnw.cmd -Dtest=BlogBackendApplicationTests test`
3. `.\mvnw.cmd spring-boot:run`
4. 打开 `http://localhost:8000/doc.html` 或 `http://localhost:8000/swagger-ui.html`
5. 视改动范围再执行 `.\mvnw.cmd test`

## 接口与文档

- 认证与系统管理接口说明：`docs/api文档/auth-api.md`
- 内容域接口说明：`docs/api文档/content-api.md`
- 文件模块接口说明：`docs/api文档/file-api.md`
- 内容域任务拆分：`docs/tasks/content-domain/`
- Swagger / Knife4j：
  - `http://localhost:8000/doc.html`
  - `http://localhost:8000/swagger-ui.html`

## 当前维护建议

- 在已有 `test` profile 基础上，继续把更多集成测试接入隔离环境。
- 为 `module/article` 补控制器与集成测试，尽快让内容域从“任务拆分完成”进入“接口可验收”。
- 将 `HELP.md` 的模板内容逐步淘汰，统一以本 README 作为项目入口说明。


