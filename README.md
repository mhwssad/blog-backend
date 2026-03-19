# blog-backend

`blog-backend` 是一个基于 Spring Boot 4、Spring Security、MyBatis-Plus、MySQL、Redis 的博客后端项目，当前仓库重点已经落地认证鉴权、RBAC 后台管理、通知中心和内容域基础设施，文章内容域仍在继续补齐接口与测试。

## 项目情况

- 技术栈：Java 17、Spring Boot 4.0.3、Spring Security、MyBatis-Plus、Druid、Redis、Knife4j/OpenAPI、MapStruct、Lombok。
- 当前默认环境：`dev`，启动端口 `8000`。
- 当前已落地的业务主线：
  - 认证与会话：账号登录、注册、邮箱验证码登录、刷新令牌、退出登录。
  - RBAC 管理：用户、角色、菜单、系统配置、通知、日志后台接口。
  - 用户侧能力：当前用户信息、当前用户菜单、用户通知中心。
- 当前在建部分：
  - `module/article` 已有 domain、mapper、service 以及 SQL 脚本。
  - `docs/tasks/content-domain/` 已拆出内容域实施任务，但控制器层尚未完整落地。
  - `module/file` 目录已预留，当前没有实际实现。

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
│  ├─ article       文章内容域基础服务
│  └─ file          预留模块
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

当前仓库的测试入口很少，只有一个 Spring 上下文加载测试：

- `src/test/java/com/cybzacg/blogbackend/BlogBackendApplicationTests.java`

运行命令：

```powershell
.\mvnw.cmd test
```

或者：

```powershell
mvn test
```

测试注意点：

- 这是 `@SpringBootTest` 集成测试，不是纯单元测试。
- 由于仓库目前没有单独的 `test` profile，测试默认会吃当前应用配置。
- 如果本地 MySQL/Redis 配置不通，`contextLoads` 大概率会在启动阶段失败。
- 目前测试覆盖率很低，更多验证仍依赖接口联调、SQL 初始化和手工回归。

如果只是想先做一轮基础自检，建议顺序是：

1. `.\mvnw.cmd -q -DskipTests compile`
2. `.\mvnw.cmd spring-boot:run`
3. 打开 `http://localhost:8000/doc.html` 或 `http://localhost:8000/swagger-ui.html`
4. 再执行 `.\mvnw.cmd test`

## 接口与文档

- 认证与 RBAC 接口说明：`docs/auth-api.md`
- 内容域任务拆分：`docs/tasks/content-domain/`
- Swagger / Knife4j：
  - `http://localhost:8000/doc.html`
  - `http://localhost:8000/swagger-ui.html`

## 当前维护建议

- 先补 `test` profile，把测试与开发数据库/Redis 配置解耦。
- 为 `module/article` 补控制器与集成测试，尽快让内容域从“任务拆分完成”进入“接口可验收”。
- 将 `HELP.md` 的模板内容逐步淘汰，统一以本 README 作为项目入口说明。
