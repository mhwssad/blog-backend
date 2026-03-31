# Auth 模块 Repository 迁移计划

## 模块信息

- **优先级**：第3轮
- **复杂度**：中
- **前置依赖**：无（基础模块，被广泛依赖）
- **涉及薄服务**：9个
- **涉及业务服务**：8个
- **数据访问总数**：约80处

## Repository 列表

| Repository 接口 | 对应实体 | 薄服务来源 | Mapper自定义方法 |
|---|---|---|---|
| `SysUserRepository` | SysUser | SysUserService | `selectByUsername`, `selectByEmail`, `updateLoginInfo` |
| `SysRoleRepository` | SysRole | SysRoleService | `selectRoleCodesByUserId` |
| `SysMenuRepository` | SysMenu | SysMenuService | `selectPermissionsByUserId`, `selectMenusByUserId` |
| `SysConfigRepository` | SysConfig | SysConfigService | `selectByConfigKey` |
| `SysLogRepository` | SysLog | SysLogService | 无 |
| `SysNoticeRepository` | SysNotice | SysNoticeService | 无 |
| `SysUserNoticeRepository` | SysUserNotice | SysUserNoticeService | 无 |
| `SysRoleMenuRepository` | SysRoleMenu | SysRoleMenuService | 无 |
| `SysUserRoleRepository` | SysUserRole | SysUserRoleService | 无 |

## 各 Repository 方法设计

### SysUserRepository

```java
public interface SysUserRepository extends IService<SysUser> {
    // Mapper XML 包装
    SysUser findByUsername(String username);
    SysUser findByEmail(String email);
    boolean updateLoginInfo(Long userId, String ip);

    // lambdaQuery 提取
    boolean existsActiveByIdentity(String identity);
    boolean existsActiveByField(String fieldName, String value);
    boolean existsActiveByUsername(String username, Long excludeId);
    boolean existsActiveByEmail(String email, Long excludeId);
    boolean existsActiveByPhone(String phone, Long excludeId);
    long countActiveByIds(Collection<Long> ids);
    Page<SysUser> pageByAdminConditions(SysUserAdminPageQuery query);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `findByUsername` | SysUserServiceImpl:20, `baseMapper.selectByUsername` | 按用户名查用户 |
| `findByEmail` | SysUserServiceImpl:25, `baseMapper.selectByEmail` | 按邮箱查用户 |
| `updateLoginInfo` | SysUserServiceImpl:30, `baseMapper.updateLoginInfo` | 更新登录IP/时间 |
| `existsActiveByIdentity` | AuthServiceImpl:320 | 注册时检查username/email/phone唯一 |
| `existsActiveByField` | AuthServiceImpl:413 | 按字段检查用户存在 |
| `existsActiveByUsername` | SysUserAdminServiceImpl:140 | 管理端用户名唯一性校验 |
| `existsActiveByEmail` | SysUserAdminServiceImpl:141 | 邮箱唯一性校验 |
| `existsActiveByPhone` | SysUserAdminServiceImpl:142 | 手机号唯一性校验 |
| `countActiveByIds` | SysNoticeAdminServiceImpl:185 | 统计有效用户数 |
| `pageByAdminConditions` | SysUserAdminServiceImpl:42 | 管理端多条件分页 |

> `save`、`updateById`、`getById` 继承自 IService。

### SysRoleRepository

```java
public interface SysRoleRepository extends IService<SysRole> {
    List<String> findRoleCodesByUserId(Long userId); // Mapper XML
    boolean existsActiveByName(String name, Long excludeId);
    boolean existsActiveByCode(String code, Long excludeId);
    long countActiveByIds(Collection<Long> ids);
    Page<SysRole> pageByAdminConditions(SysRoleAdminPageQuery query);
}
```

### SysMenuRepository

```java
public interface SysMenuRepository extends IService<SysMenu> {
    List<String> findPermissionsByUserId(Long userId); // Mapper XML 多表JOIN
    List<SysMenu> findMenusByUserId(Long userId);     // Mapper XML 多表JOIN
    List<SysMenu> findAllOrdered();
    List<SysMenu> findByParentId(Long parentId);
    boolean existsByParentId(Long parentId);
    long countByIds(Collection<Long> ids);
}
```

### SysConfigRepository

```java
public interface SysConfigRepository extends IService<SysConfig> {
    SysConfig findByConfigKey(String configKey); // Mapper XML
    boolean existsActiveByConfigKey(String configKey, Long excludeId);
    Page<SysConfig> pageByAdminConditions(SysConfigAdminPageQuery query);
}
```

> **注意**：`SysConfigServiceImpl` 中的缓存逻辑（`getByConfigKey`、`getValueOrDefault`、`evictConfigCache`）属于 Service 层关注点，不迁移到 Repository。Service 注入 Repository 获取底层数据，自行管理缓存。

### SysLogRepository（特殊处理）

```java
public interface SysLogRepository extends IService<SysLog> {
    Page<SysLog> pageByAdminConditions(SysLogAdminPageQuery query);
    long countByConditions(SysLogCleanRequest request);
    long removeByConditions(SysLogCleanRequest request);

    // 特殊：保留 REQUIRES_NEW 传播行为
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    default boolean saveLog(SysLog log) { return save(log); }
}
```

> **唯一例外**：`SysLogServiceImpl.saveLog()` 使用 `REQUIRES_NEW` 传播行为，这是数据层特性（确保日志独立事务），随 Repository 迁移。

### SysNoticeRepository

```java
public interface SysNoticeRepository extends IService<SysNotice> {
    Page<SysNotice> pageByAdminConditions(SysNoticeAdminPageQuery query);
    Page<SysNotice> pageInboxNotices(InboxNoticeQuery query);
    long countGlobalUnread(Collection<Long> readNoticeIds);
    long countTargetedUnread(Collection<Long> unreadTargetNoticeIds);
    List<SysNotice> findGlobalUnread(Collection<Long> existingNoticeIds);
}
```

### SysUserNoticeRepository

```java
public interface SysUserNoticeRepository extends IService<SysUserNotice> {
    void deleteByNoticeId(Long noticeId);
    List<SysUserNotice> findByUserId(Long userId);
    Optional<SysUserNotice> findLatestByNoticeIdAndUserId(Long noticeId, Long userId);
    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);
}
```

### SysRoleMenuRepository

```java
public interface SysRoleMenuRepository extends IService<SysRoleMenu> {
    List<Long> findMenuIdsByRoleId(Long roleId);
    void deleteByRoleId(Long roleId);
    void deleteByMenuId(Long menuId);
}
```

### SysUserRoleRepository

```java
public interface SysUserRoleRepository extends IService<SysUserRole> {
    List<Long> findRoleIdsByUserId(Long userId);
    void deleteByUserId(Long userId);
    void deleteByRoleId(Long roleId);
}
```

## 跨模块依赖

### 被其他模块注入

| 薄服务 | 外部注入方 |
|---|---|
| SysUserService | follow(1), chat(2+), article(2), content(1+) |
| SysRoleService | 仅本模块 |
| SysMenuService | 仅本模块 |

> `SysUserService` 是被依赖最广泛的薄服务。本模块迁移后旧服务保留，待其他模块各自迁移时替换为 `SysUserRepository`。

## 执行步骤

### Step 1: 创建9个 Repository 接口 + 9个实现

### Step 2: 修改8个业务服务

1. `AuthServiceImpl` — 注入 `SysUserRepository`, `SysRoleRepository`, `SysMenuRepository`, `SysConfigRepository`
2. `SysUserAdminServiceImpl` — 注入 `SysUserRepository`, `SysRoleRepository`, `SysUserRoleRepository`
3. `SysRoleAdminServiceImpl` — 注入 `SysRoleRepository`, `SysRoleMenuRepository`, `SysMenuRepository`
4. `SysMenuAdminServiceImpl` — 注入 `SysMenuRepository`
5. `SysConfigAdminServiceImpl` — 注入 `SysConfigRepository`
6. `SysLogAdminServiceImpl` — 注入 `SysLogRepository`
7. `SysNoticeAdminServiceImpl` — 注入 `SysNoticeRepository`, `SysUserNoticeRepository`, `SysUserRepository`
8. `UserNoticeInboxServiceImpl` — 注入 `SysNoticeRepository`, `SysUserNoticeRepository`

### Step 3: 更新测试

### Step 4: 删除旧薄服务（等所有依赖模块迁移完成后）

## 验证

```bash
mvn compile -q
mvn test -Dtest="com.cybzacg.blogbackend.module.auth.*Test"
```
