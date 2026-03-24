# API文档总览

## 文档分布

- [`auth-api.md`](/e:/project/blog/blog-backend/docs/api文档/auth-api.md)：认证与系统管理模块接口文档，覆盖 `auth` 模块下全部控制器。
- [`content-api.md`](/e:/project/blog/blog-backend/docs/api文档/content-api.md)：内容域接口文档，统一覆盖 `article` 与 `content` 模块下全部控制器。
- [`file-api.md`](/e:/project/blog/blog-backend/docs/api文档/file-api.md)：文件上传与后台文件管理接口文档，覆盖 `file` 模块下全部控制器。

## 模块扫描结果

本次按 `src/main/java/com/cybzacg/blogbackend/module` 目录扫描控制器，结果如下：

| 模块 | 控制器情况 | 文档归属 | 备注 |
|----|------|------|----|
| `auth` | 已发现认证、用户、角色、菜单、配置、通知、日志、用户通知中心控制器 | [`auth-api.md`](/e:/project/blog/blog-backend/docs/api文档/auth-api.md) | 已覆盖 |
| `article` | 已发现后台文章、前台文章、用户文章点赞控制器 | [`content-api.md`](/e:/project/blog/blog-backend/docs/api文档/content-api.md) | 作为内容域的一部分统一编写 |
| `content` | 已发现后台分类/标签/评论/收藏/互动/足迹，以及前台分类/标签/评论与用户行为控制器 | [`content-api.md`](/e:/project/blog/blog-backend/docs/api文档/content-api.md) | 已覆盖 |
| `file` | 已发现后台文件管理与用户文件上传控制器 | [`file-api.md`](/e:/project/blog/blog-backend/docs/api文档/file-api.md) | 已覆盖 |

## 统一约定

- `/api/auth/**`：认证相关接口
- `/api/sys/**`：后台管理接口
- `/api/user/**`：登录用户行为接口
- 前台公开只读接口统一放在非 `/api/sys/**`、非 `/api/user/**` 路径下

## 维护说明

- 新增控制器时，先更新本总览，再补对应模块文档。
- `file` 模块接口较多，独立维护 `file-api.md`。
- 仓库根目录 `README.md` 中的“接口与文档”入口应与本目录保持一致。
