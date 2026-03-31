# Follow 模块 Repository 迁移计划

## 模块信息

- **优先级**：第1轮（原型验证）
- **复杂度**：低
- **前置依赖**：无
- **涉及薄服务**：1个
- **涉及业务服务**：1个

## 当前数据访问现状

### 薄服务（Tier-1）

| 服务 | 位置 | 自定义方法 |
|---|---|---|
| `SysUserFollowService` | `module/follow/service/` | 无（空壳） |
| `SysUserFollowServiceImpl` | `module/follow/service/impl/` | 无（空壳） |

### 业务服务数据访问分析

**`UserFollowServiceImpl`**（`module/follow/service/impl/UserFollowServiceImpl.java`）

该服务直接注入 `SysUserFollowMapper`，所有数据操作通过 Mapper 完成：

| 行号 | 当前调用 | 操作类型 | 迁移到 Repository 方法 |
|---|---|---|---|
| 79 | `sysUserFollowMapper.updateById(relation)` | Mapper直接调用 | `updateById(relation)` — 继承自IService |
| 88 | `sysUserFollowMapper.countFollowPage(userId, specialOnly)` | Mapper XML | `countFollowPage(userId, specialOnly)` |
| 94 | `sysUserFollowMapper.selectFollowPage(userId, specialOnly, offset, size)` | Mapper XML | `selectFollowPage(userId, specialOnly, offset, size)` |
| 109 | `sysUserFollowMapper.countFanPage(userId)` | Mapper XML | `countFanPage(userId)` |
| 114 | `sysUserFollowMapper.selectFanPage(userId, offset, size)` | Mapper XML | `selectFanPage(userId, offset, size)` |
| 127 | `sysUserFollowMapper.countActiveRelation(userId, targetUserId)` | Mapper XML | `countActiveRelation(followerId, followingId)` |
| 128 | `sysUserFollowMapper.countActiveRelation(targetUserId, userId)` | Mapper XML | 同上 |
| 141 | `sysUserFollowMapper.countActiveFollowing(userId)` | Mapper XML | `countActiveFollowing(userId)` |
| 142 | `sysUserFollowMapper.countActiveFans(userId)` | Mapper XML | `countActiveFans(userId)` |
| 156 | `sysUserFollowMapper.updateById(relation)` | Mapper直接调用 | `updateById(relation)` — 继承自IService |
| 168 | `sysUserFollowMapper.updateById(relation)` | Mapper直接调用 | `updateById(relation)` — 继承自IService |
| 177 | `sysUserFollowMapper.insert(created)` | Mapper直接调用 | `save(entity)` — 继承自IService |
| 197 | `sysUserFollowMapper.updateById(relation)` | Mapper直接调用 | `updateById(relation)` — 继承自IService |
| 212 | `sysUserFollowMapper.selectOne(Wrappers.lambdaQuery(SysUserFollow.class).eq(...).eq(...).last("limit 1"))` | Wrapper查询 | `findByFollowerAndFollowing(followerId, followingId)` |

### 跨模块依赖

- `SysUserService`（auth模块）：调用 `getById()` 检查目标用户是否存在 — **注入 `SysUserRepository`**

## 执行步骤

### Step 1: 创建 Repository 接口

**文件**：`src/main/java/com/cybzacg/blogbackend/module/follow/repository/SysUserFollowRepository.java`

```java
package com.cybzacg.blogbackend.module.follow.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysUserFollow;
import com.cybzacg.blogbackend.module.follow.model.data.FollowRelationUserItem;
import java.util.List;

/**
 * 用户关注关系数据访问层。
 */
public interface SysUserFollowRepository extends IService<SysUserFollow> {

    /**
     * 根据关注人和被关注人查询单条关系。
     */
    SysUserFollow findByFollowerAndFollowing(Long followerId, Long followingId);

    /**
     * 以下方法包装 Mapper XML 自定义SQL。
     */
    Long countFollowPage(Long userId, Boolean specialOnly);
    List<FollowRelationUserItem> selectFollowPage(Long userId, Boolean specialOnly, Long offset, Long size);
    Long countFanPage(Long userId);
    List<FollowRelationUserItem> selectFanPage(Long userId, Long offset, Long size);
    Long countActiveRelation(Long followerId, Long followingId);
    Long countActiveFollowing(Long userId);
    Long countActiveFans(Long userId);
}
```

### Step 2: 创建 Repository 实现

**文件**：`src/main/java/com/cybzacg/blogbackend/module/follow/repository/impl/SysUserFollowRepositoryImpl.java`

```java
package com.cybzacg.blogbackend.module.follow.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysUserFollow;
import com.cybzacg.blogbackend.mapper.SysUserFollowMapper;
import com.cybzacg.blogbackend.module.follow.model.data.FollowRelationUserItem;
import com.cybzacg.blogbackend.module.follow.repository.SysUserFollowRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 用户关注关系数据访问层实现。
 */
@Service
public class SysUserFollowRepositoryImpl extends ServiceImpl<SysUserFollowMapper, SysUserFollow>
        implements SysUserFollowRepository {

    @Override
    public SysUserFollow findByFollowerAndFollowing(Long followerId, Long followingId) {
        return getOne(Wrappers.lambdaQuery(SysUserFollow.class)
                .eq(SysUserFollow::getFollowerId, followerId)
                .eq(SysUserFollow::getFollowingId, followingId)
                .last("limit 1"));
    }

    @Override
    public Long countFollowPage(Long userId, Boolean specialOnly) {
        return baseMapper.countFollowPage(userId, specialOnly);
    }

    @Override
    public List<FollowRelationUserItem> selectFollowPage(Long userId, Boolean specialOnly, Long offset, Long size) {
        return baseMapper.selectFollowPage(userId, specialOnly, offset, size);
    }

    @Override
    public Long countFanPage(Long userId) {
        return baseMapper.countFanPage(userId);
    }

    @Override
    public List<FollowRelationUserItem> selectFanPage(Long userId, Long offset, Long size) {
        return baseMapper.selectFanPage(userId, offset, size);
    }

    @Override
    public Long countActiveRelation(Long followerId, Long followingId) {
        return baseMapper.countActiveRelation(followerId, followingId);
    }

    @Override
    public Long countActiveFollowing(Long userId) {
        return baseMapper.countActiveFollowing(userId);
    }

    @Override
    public Long countActiveFans(Long userId) {
        return baseMapper.countActiveFans(userId);
    }
}
```

### Step 3: 修改业务服务

**文件**：`src/main/java/com/cybzacg/blogbackend/module/follow/service/impl/UserFollowServiceImpl.java`

**变更清单**：

1. **替换依赖注入**：
   - 移除 `private final SysUserFollowMapper sysUserFollowMapper;`
   - 新增 `private final SysUserFollowRepository sysUserFollowRepository;`
   - 保留 `private final SysUserService sysUserService;`（等auth模块完成后替换为 `SysUserRepository`）

2. **替换所有数据调用**：

   | 原代码 | 替换为 |
   |---|---|
   | `sysUserFollowMapper.updateById(relation)` | `sysUserFollowRepository.updateById(relation)` |
   | `sysUserFollowMapper.insert(created)` | `sysUserFollowRepository.save(created)` |
   | `sysUserFollowMapper.selectOne(Wrappers.lambdaQuery...)` | `sysUserFollowRepository.findByFollowerAndFollowing(userId, targetUserId)` |
   | `sysUserFollowMapper.countFollowPage(...)` | `sysUserFollowRepository.countFollowPage(...)` |
   | `sysUserFollowMapper.selectFollowPage(...)` | `sysUserFollowRepository.selectFollowPage(...)` |
   | `sysUserFollowMapper.countFanPage(...)` | `sysUserFollowRepository.countFanPage(...)` |
   | `sysUserFollowMapper.selectFanPage(...)` | `sysUserFollowRepository.selectFanPage(...)` |
   | `sysUserFollowMapper.countActiveRelation(...)` | `sysUserFollowRepository.countActiveRelation(...)` |
   | `sysUserFollowMapper.countActiveFollowing(...)` | `sysUserFollowRepository.countActiveFollowing(...)` |
   | `sysUserFollowMapper.countActiveFans(...)` | `sysUserFollowRepository.countActiveFans(...)` |

3. **移除不再需要的 import**：
   - `com.baomidou.mybatisplus.core.toolkit.Wrappers`
   - `com.cybzacg.blogbackend.mapper.SysUserFollowMapper`

### Step 4: 更新测试

**测试文件**：`src/test/java/com/cybzacg/blogbackend/module/follow/` 下的测试

- 将 `@Mock SysUserFollowMapper` 替换为 `@Mock SysUserFollowRepository`
- 构造函数参数替换
- 移除 `Wrappers` / `LambdaQueryWrapper` 相关 mock
- 简化 mock setup（不再需要 mock 链式调用）

### Step 5: 删除旧薄服务

确认所有迁移完成后删除：

- `src/main/java/com/cybzacg/blogbackend/module/follow/service/SysUserFollowService.java`
- `src/main/java/com/cybzacg/blogbackend/module/follow/service/impl/SysUserFollowServiceImpl.java`

检查是否有其他模块注入了 `SysUserFollowService`，若有则同步替换为 `SysUserFollowRepository`。

## 文件清单

| 操作 | 文件路径 |
|---|---|
| 新建 | `module/follow/repository/SysUserFollowRepository.java` |
| 新建 | `module/follow/repository/impl/SysUserFollowRepositoryImpl.java` |
| 修改 | `module/follow/service/impl/UserFollowServiceImpl.java` |
| 修改 | 对应测试文件 |
| 删除 | `module/follow/service/SysUserFollowService.java` |
| 删除 | `module/follow/service/impl/SysUserFollowServiceImpl.java` |

## 验证

```bash
mvn compile -q
mvn test -Dtest="com.cybzacg.blogbackend.module.follow.*Test"
```

确认 `UserFollowServiceImpl` 中：
- 无 `lambdaQuery()` / `lambdaUpdate()` 调用
- 无 `LambdaQueryWrapper` / `Wrappers` 使用
- 无 Mapper 直接注入
- 无 `import com.cybzacg.blogbackend.mapper.*`
