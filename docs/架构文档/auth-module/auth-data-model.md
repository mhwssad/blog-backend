# Auth 数据模型

## 1. 数据库表结构

### 1.1 用户表 (sys_user)

```sql
CREATE TABLE sys_user (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL COMMENT '用户名',
    password        VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname        VARCHAR(64)  COMMENT '昵称',
    email           VARCHAR(128) COMMENT '邮箱',
    phone          VARCHAR(32)  COMMENT '手机号',
    avatar         VARCHAR(512) COMMENT '头像URL',
    bio            VARCHAR(500) COMMENT '个人简介',
    website        VARCHAR(256) COMMENT '个人站点',
    gender         TINYINT      DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女，3-保密',
    birthday       DATE         COMMENT '生日',
    status         TINYINT      DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    user_level     INT          DEFAULT 1 COMMENT '用户等级：1-10',
    experience_points INT       DEFAULT 0 COMMENT '经验值',
    level_updated_at DATETIME   COMMENT '最近一次等级变更时间',
    last_login_time DATETIME   COMMENT '最后登录时间',
    last_login_ip  VARCHAR(64) COMMENT '最后登录IP',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_flag   TINYINT      DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    remark         VARCHAR(512) COMMENT '备注',
    mfa_enabled    TINYINT      DEFAULT 0 COMMENT 'MFA是否启用：0-否，1-是',
    UNIQUE KEY uk_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

### 1.2 角色表 (sys_role)

```sql
CREATE TABLE sys_role (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL COMMENT '角色名称',
    code            VARCHAR(64)  NOT NULL COMMENT '角色编码',
    sort            INT          DEFAULT 0 COMMENT '显示顺序',
    status          TINYINT      DEFAULT 1 COMMENT '角色状态：1-正常，0-停用',
    data_scope      TINYINT      DEFAULT 1 COMMENT '数据权限：1-所有数据，2-部门及子部门，3-本部门，4-本人，5-自定义',
    create_by       BIGINT       COMMENT '创建人ID',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT       COMMENT '更新人ID',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    UNIQUE KEY uk_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';
```

### 1.3 菜单表 (sys_menu)

```sql
CREATE TABLE sys_menu (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    parent_id       BIGINT       DEFAULT 0 COMMENT '父菜单ID',
    tree_path       VARCHAR(255) COMMENT '父节点ID路径',
    name            VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    type            CHAR(1)      NOT NULL COMMENT '菜单类型：C-目录，M-菜单，B-按钮',
    route_name      VARCHAR(64)  COMMENT '路由名称',
    route_path      VARCHAR(255) COMMENT '路由路径',
    component       VARCHAR(255) COMMENT '组件路径',
    perm            VARCHAR(100) COMMENT '按钮权限标识',
    always_show     TINYINT      DEFAULT 0 COMMENT '是否始终显示：1-是，0-否',
    keep_alive      TINYINT      DEFAULT 0 COMMENT '是否开启缓存：1-是，0-否',
    visible         TINYINT      DEFAULT 1 COMMENT '显示状态：1-显示，0-隐藏',
    sort            INT          DEFAULT 0 COMMENT '排序',
    icon            VARCHAR(64)  COMMENT '菜单图标',
    redirect        VARCHAR(255) COMMENT '跳转路径',
    params          TEXT         COMMENT '路由参数',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';
```

### 1.4 用户角色关联表 (sys_user_role)

```sql
CREATE TABLE sys_user_role (
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    role_id         BIGINT       NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

### 1.5 角色菜单关联表 (sys_role_menu)

```sql
CREATE TABLE sys_role_menu (
    role_id         BIGINT       NOT NULL COMMENT '角色ID',
    menu_id         BIGINT       NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';
```

### 1.6 作者申请表 (sys_author_application)

```sql
CREATE TABLE sys_author_application (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL COMMENT '申请用户ID',
    apply_status    TINYINT      DEFAULT 0 COMMENT '申请状态：0-待审核，1-已通过，2-已拒绝，3-待补充',
    apply_reason    VARCHAR(500) COMMENT '申请说明',
    content_direction VARCHAR(256) COMMENT '擅长内容方向',
    introduction    VARCHAR(500) COMMENT '个人简介',
    sample_links_json TEXT       COMMENT '示例链接JSON数组',
    reviewer_id     BIGINT       COMMENT '审核人ID',
    review_comment  VARCHAR(500) COMMENT '审核备注',
    submitted_at    DATETIME     COMMENT '提交时间',
    reviewed_at     DATETIME     COMMENT '审核时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_apply_status (apply_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作者申请表';
```

### 1.7 用户经验流水表 (user_experience_log)

```sql
CREATE TABLE user_experience_log (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    source_type     VARCHAR(32)  NOT NULL COMMENT '经验来源类型',
    source_biz_id   VARCHAR(64)  COMMENT '来源业务ID',
    xp_value        INT          NOT NULL COMMENT '经验值变化量',
    idempotent_key  VARCHAR(64)  COMMENT '幂等键',
    log_date        DATE         NOT NULL COMMENT '日志日期',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_source_type (source_type),
    INDEX idx_log_date (log_date),
    UNIQUE KEY uk_idempotent (idempotent_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户经验流水表';
```

## 2. 实体类映射

| 表名 | 实体类 | 包路径 |
|------|--------|--------|
| sys_user | SysUser | `domain/auth/SysUser.java` |
| sys_role | SysRole | `domain/auth/SysRole.java` |
| sys_menu | SysMenu | `domain/auth/SysMenu.java` |
| sys_user_role | SysUserRole | `domain/auth/SysUserRole.java` |
| sys_role_menu | SysRoleMenu | `domain/auth/SysRoleMenu.java` |
| sys_author_application | SysAuthorApplication | `domain/auth/SysAuthorApplication.java` |
| user_experience_log | UserExperienceLog | `domain/auth/UserExperienceLog.java` |

## 3. 数据流关系

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   SysUser   │────▶│ SysUserRole │◀────│   SysRole   │
└─────────────┘     └─────────────┘     └─────────────┘
       │                                       │
       │                                       │
       ▼                                       ▼
┌─────────────┐                         ┌─────────────┐
│  Experience │                         │  SysRoleMenu│
│    Log       │                         └─────────────┘
└─────────────┘                               │
                                                │
                                                ▼
                                          ┌─────────────┐
                                          │   SysMenu   │
                                          └─────────────┘

┌─────────────────────┐     ┌─────────────────────┐
│SysAuthorApplication │────▶│      SysUser        │
└─────────────────────┘     └─────────────────────┘
```

## 4. 枚举值定义

### 4.1 作者申请状态 (AuthorApplicationStatusEnum)

| 值 | 标签 | 说明 |
|----|------|------|
| 0 | PENDING | 待审核 |
| 1 | APPROVED | 已通过 |
| 2 | REJECTED | 已拒绝 |
| 3 | NEED_MORE_INFO | 待补充 |

### 4.2 通知类型 (NotificationTypeEnum)

| Code | 标签 | 说明 |
|------|------|------|
| comment_me | 评论我 | 评论通知 |
| like_me | 点赞我 | 点赞通知 |
| collect_article | 收藏我文章 | 收藏通知 |
| follow_me | 有人关注我 | 关注通知 |
| private_message | 收到私聊 | 私聊通知 |
| group_mention | 群聊有人@我 | 群聊@通知 |
| channel_announcement | 频道公告 | 频道公告 |
| system_announcement | 系统公告 | 系统公告 |
| ai_task_done | AI任务完成 | AI任务通知 |
| report_result | 举报处理结果 | 举报结果通知 |
| forum_post_essence | 论坛帖子设为精华 | 精华通知 |
| forum_reply_me | 论坛回复我 | 论坛回复通知 |
| forum_like_me | 论坛点赞我 | 论坛点赞通知 |

### 4.3 用户性别 (GenderEnum)

| 值 | 标签 |
|----|------|
| 0 | 未知 |
| 1 | 男 |
| 2 | 女 |
| 3 | 保密 |

### 4.4 用户状态 (UserStatusEnum)

| 值 | 标签 | 说明 |
|----|------|------|
| 0 | DISABLED | 禁用 |
| 1 | ENABLED | 启用 |

### 4.5 菜单类型 (MenuTypeEnum)

| 值 | 标签 | 说明 |
|----|------|------|
| C | CATALOG | 目录 |
| M | MENU | 菜单 |
| B | BUTTON | 按钮 |

### 4.6 数据权限范围 (DataScopeEnum)

| 值 | 标签 | 说明 |
|----|------|------|
| 1 | ALL | 所有数据 |
| 2 | DEPT_AND_CHILD | 部门及子部门数据 |
| 3 | DEPT | 本部门数据 |
| 4 | SELF | 本人数据 |
| 5 | CUSTOM | 自定义部门数据 |
