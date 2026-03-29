# 数据库表设计修复文档

本文档用于沉淀当前 `blog-backend` 数据库表设计的已确认问题、修复边界、执行顺序和后续分期策略，作为存量库升级与新库初始化对齐的统一依据。

## 1. 修复背景

- 截至 2026-03-28，仓库内已经存在 `1.sys.sql`、`02_article.sql`、`04_file.sql`、`05_chat.sql`、`06_follow.sql` 等脚本，但实际 `blog_backend` 库未完全与仓库脚本保持同步。
- 实库此前缺少文件域表，以及聊天、粉丝关注预留表；这会导致“代码能力已存在 / 数据库基础未补齐”的状态不一致。
- 系统基础表中还存在一批不会立刻导致编译失败，但会在业务扩展、数据治理和线上稳定性上持续放大的结构问题。

## 2. 当前已确认问题

### 2.1 P0：本轮必须收口的问题

- `sys_user` 仍使用全局 `uk_username` 唯一索引，与逻辑删除设计不一致。
- `sys_user` 的邮箱、手机号只做普通索引，没有数据库层“有效用户唯一性”约束。
- `sys_role` 仍使用全局 `uk_name`、`uk_code` 唯一索引，与逻辑删除设计不一致。
- `sys_user_notice` 缺少 `(notice_id, user_id)` 唯一约束，存在重复投递关系风险。
- `file_business_info` 旧唯一约束未带 `user_id`，会把不同用户对同一业务对象的文件引用错误折叠到同一条记录。
- `sys_user_notice` 缺少面向收件箱查询的用户侧索引，当前分页与未读统计会越来越依赖全表扫描。
- `sys_notice` 缺少围绕发布时间、发布状态、目标范围的关键索引，后台列表和用户收件箱都没有稳定索引路径。
- `sys_config` 只在服务层校验 `config_key` 唯一，没有数据库层约束兜底。
- `sys_log` 仍是 `MyISAM`，与其余核心表不一致，不利于事务型系统内的统一维护。
- 实库缺少 `file_*`、`chat_*`、`sys_user_follow` 表，无法与当前仓库能力和后续扩展预留保持一致。

### 2.2 P1：需要进入后续分期的问题

- `sys_notice.target_user_ids` 与 `sys_user_notice` 并存，属于“双数据源”设计，当前为了兼容现有 Java 代码暂不移除。
- 多张表的时间字段命名存在 `create_time` / `created_at` 并存问题，本轮不做大范围重命名，避免影响实体和 Mapper。
- 各脚本字符集 / 排序规则仍有历史差异，本轮只保证新增脚本与原模块脚本兼容，不做全量统一。

### 2.3 P2：需要评估后再决定的问题

- `sys_role_menu`、`sys_user_role` 是否引入代理主键，目前不作为本轮修复项；当前实体与服务实现仍按关系表模式使用。
- 通知投递关系是否保留逻辑删除字段，当前先不做语义重构。

## 3. 本轮修复原则

- 只做与现有代码兼容的增量修复，不做会引发实体、Mapper、Service 大面积跟改的重构。
- 空库初始化继续依赖现有分模块脚本；存量库升级统一走新增的 `07_schema_repair.sql`。
- 对已确认的唯一性要求，优先在数据库层补约束，避免只靠服务层校验。
- 对当前高频查询路径，优先补必要索引，不追求一次性把所有历史表全部重构。

## 4. 本轮执行内容

### 4.1 存量库同步

- 为已有存量库新增缺失的文件域表：`file_info`、`file_upload_task`、`file_chunk`、`file_business_info`
- 为已有存量库新增缺失的聊天预留表：`chat_conversation`、`chat_conversation_member`、`chat_message`、`chat_message_recipient`、`chat_message_read_cursor`
- 为已有存量库新增缺失的粉丝关注表：`sys_user_follow`

说明：

- `04_file.sql`、`05_chat.sql`、`06_follow.sql` 仍保留为“空库初始化脚本”。
- 由于这些脚本中包含 `DROP TABLE IF EXISTS`，不适合直接对存量库执行；存量库必须改走 `07_schema_repair.sql`。

### 4.2 老表兼容性修复

- `sys_user`
- 去掉与软删除不兼容的全局用户名唯一索引。
- 新增“有效用户名 / 有效邮箱 / 有效手机号”生成列与唯一索引，只约束 `deleted_flag = 0` 的有效用户。
- 新增 `deleted_flag + username` 查询索引，补回后台用户与认证链路的稳定检索路径。
- `sys_role`
- 去掉与软删除不兼容的全局角色名称 / 编码唯一索引。
- 新增“有效角色名称 / 有效角色编码”生成列与唯一索引，只约束 `is_deleted = 0` 的有效角色。
- 新增 `is_deleted + name`、`is_deleted + code` 查询索引，补回后台角色管理的稳定检索路径。
- `sys_user_notice`
- 新增 `(notice_id, user_id)` 唯一约束。
- `file_business_info`
- 将 `uk_file_reference` 调整为 `(file_id, user_id, reference_type, reference_id)`，避免跨用户错误复用同一业务引用。
- 新增面向收件箱分页与未读统计的 `user_id + is_deleted + is_read + notice_id` 索引。
- `sys_notice`
- 新增后台列表索引：`is_deleted + create_time + id`。
- 新增收件箱索引：`is_deleted + publish_status + target_type + publish_time + id`。
- `sys_config`
- 新增“有效配置键”生成列与唯一索引，只约束 `is_deleted = 0` 的有效配置。
- 新增 `config_key + is_deleted` 查询索引。
- `sys_log`
- 引擎统一调整为 `InnoDB`。
- `sys_role_menu`
- 补 `menu_id` 反向索引，支撑按菜单清理角色关系。
- `sys_user_role`
- 补 `role_id` 反向索引，支撑按角色清理用户关系。

## 5. 执行顺序

建议顺序：

1. 先检查存量库是否存在活跃数据冲突。
2. 执行 `src/main/resources/mysql/07_schema_repair.sql`。
3. 校验缺失表是否已补齐、关键索引是否存在、日志表引擎是否已切换。
4. 对新环境继续按空库初始化脚本执行，避免把升级脚本当作初始化脚本使用。

## 6. 执行前检查项

建议至少执行以下检查：

```sql
SELECT username, COUNT(*) AS cnt
FROM sys_user
WHERE deleted_flag = 0
GROUP BY username
HAVING cnt > 1;

SELECT email, COUNT(*) AS cnt
FROM sys_user
WHERE deleted_flag = 0
  AND email IS NOT NULL
  AND TRIM(email) <> ''
GROUP BY email
HAVING cnt > 1;

SELECT phone, COUNT(*) AS cnt
FROM sys_user
WHERE deleted_flag = 0
  AND phone IS NOT NULL
  AND TRIM(phone) <> ''
GROUP BY phone
HAVING cnt > 1;

SELECT notice_id, user_id, COUNT(*) AS cnt
FROM sys_user_notice
WHERE is_deleted = 0
GROUP BY notice_id, user_id
HAVING cnt > 1;

SELECT name, COUNT(*) AS cnt
FROM sys_role
WHERE is_deleted = 0
GROUP BY name
HAVING cnt > 1;

SELECT code, COUNT(*) AS cnt
FROM sys_role
WHERE is_deleted = 0
GROUP BY code
HAVING cnt > 1;

SELECT file_id, user_id, reference_type, reference_id, COUNT(*) AS cnt
FROM file_business_info
GROUP BY file_id, user_id, reference_type, reference_id
HAVING cnt > 1;

SELECT config_key, COUNT(*) AS cnt
FROM sys_config
WHERE is_deleted = 0
GROUP BY config_key
HAVING cnt > 1;
```

当前本地开发库已完成上述检查，未发现会阻断本轮修复的重复活跃数据。

## 7. 回滚建议

- 本轮修复涉及唯一索引、生成列、存储引擎和新表补齐，推荐在执行前先做数据库备份。
- 若执行失败，优先采用“恢复执行前备份”的方式回滚，而不是手工逐条回撤。
- 新增缺失表的回滚应结合实际业务决定，避免误删已开始写入的新表数据。

## 8. 后续分期建议

### 8.1 下一阶段优先项

- 评估 `sys_notice.target_user_ids` 与 `sys_user_notice` 的单一数据源收口方案。
- 为通知、关注、聊天能力补齐明确的模块级接口文档和状态流转文档。
- 继续梳理历史脚本的时间字段命名与字符集差异，形成统一标准。

### 8.2 暂缓项

- 不在本轮推动全库命名重构。
- 不在本轮为关系表强行引入代理主键。
- 不在本轮直接修改现有 Java 业务逻辑来配合数据库大重构。
