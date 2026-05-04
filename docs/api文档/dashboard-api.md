# 后台数据看板接口文档

本文档面向前端联调，对应项目中后台数据看板模块的实现。

## 1. 当前能力范围

当前已支持：

- 核心概览指标（用户、文章、评论、消息、AI 调用、举报）
- 内容统计（文章、评论、点赞、收藏）
- 社区统计（私信消息、大厅消息、群组数量、论坛发帖/回复、热门版块）
- AI 调用统计（总调用、成功、失败、RAG 调用、Agent 任务）
- 治理统计（举报各状态数量、处理耗时、处罚分布）
- 当前时间范围运营统计 Excel 导出

## 2. 鉴权要求

后台看板接口统一走 `/api/sys/dashboard/**`，要求登录且拥有 `sys:dashboard:query` 权限：

```http
Authorization: Bearer <accessToken>
```

## 3. 公共查询参数

所有看板接口共享以下查询参数（`DashboardRangeQuery`）：

| 参数         | 类型   | 必填 | 说明                                                        |
|------------|------|------|-----------------------------------------------------------|
| `rangeType` | String | 否  | 时间范围：`today` / `week` / `month` / `all` / `custom`，默认 `today` |
| `startTime` | DateTime | 条件必填 | 自定义开始时间，`rangeType=custom` 时必填，ISO 8601 格式              |
| `endTime`   | DateTime | 条件必填 | 自定义结束时间，`rangeType=custom` 时必填，ISO 8601 格式              |

各 `rangeType` 含义：

| rangeType | 说明                                |
|-----------|-----------------------------------|
| `today`   | 今日（当天 00:00 ~ 次日 00:00）          |
| `week`    | 本周（本周一 00:00 ~ 下周一 00:00）        |
| `month`   | 本月（本月 1 日 00:00 ~ 下月 1 日 00:00）   |
| `all`     | 全部（startTime / endTime 为空）         |
| `custom`  | 自定义，必须同时传 startTime 和 endTime       |

边界说明：

- `rangeType` 传入非法值时返回 `40011 / 非法参数`。
- `rangeType=custom` 时，`startTime` 和 `endTime` 必须同时传入，且 `startTime` 必须早于 `endTime`。
- 自定义时间范围不能超过 366 天，超出返回业务错误。
- 不传 `rangeType` 时默认按 `today` 统计。

## 4. 后台管理接口

### 4.1 核心概览

- 请求：`GET /api/sys/dashboard/overview`
- 查询参数：公共查询参数
- 响应：`DashboardOverviewVO`

| 字段                        | 类型     | 说明           |
|---------------------------|--------|--------------|
| `range`                   | Object | 时间范围         |
| `range.rangeType`         | String | 时间范围类型       |
| `range.startTime`         | DateTime | 统计开始时间     |
| `range.endTime`           | DateTime | 统计结束时间     |
| `registeredUserCount`     | Long   | 注册用户数        |
| `activeUserCount`         | Long   | 活跃用户数        |
| `authorCount`             | Long   | 作者数量          |
| `articleCount`            | Long   | 文章总数          |
| `pendingArticleReviewCount` | Long | 待审核文章数       |
| `commentCount`            | Long   | 评论数           |
| `chatMessageCount`        | Long   | 私信消息数         |
| `aiCallCount`             | Long   | AI 调用次数       |
| `reportCount`             | Long   | 举报总数          |
| `pendingReportCount`      | Long   | 待处理举报数（全局）  |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "range": {
      "rangeType": "today",
      "startTime": "2026-05-03T00:00:00",
      "endTime": "2026-05-04T00:00:00"
    },
    "registeredUserCount": 1024,
    "activeUserCount": 86,
    "authorCount": 52,
    "articleCount": 15,
    "pendingArticleReviewCount": 3,
    "commentCount": 47,
    "chatMessageCount": 230,
    "aiCallCount": 68,
    "reportCount": 2,
    "pendingReportCount": 1
  }
}
```

### 4.2 内容统计

- 请求：`GET /api/sys/dashboard/content`
- 查询参数：公共查询参数
- 响应：`DashboardContentVO`

| 字段                        | 类型   | 说明       |
|---------------------------|------|----------|
| `range`                   | Object | 时间范围   |
| `articleCount`            | Long | 文章总数    |
| `pendingArticleReviewCount` | Long | 待审核文章数 |
| `commentCount`            | Long | 评论数     |
| `likeCount`               | Long | 点赞数     |
| `collectCount`            | Long | 收藏数     |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "range": {
      "rangeType": "week",
      "startTime": "2026-04-27T00:00:00",
      "endTime": "2026-05-04T00:00:00"
    },
    "articleCount": 42,
    "pendingArticleReviewCount": 3,
    "commentCount": 128,
    "likeCount": 356,
    "collectCount": 89
  }
}
```

### 4.3 社区统计

- 请求：`GET /api/sys/dashboard/community`
- 查询参数：公共查询参数
- 响应：`DashboardCommunityVO`

| 字段                 | 类型   | 说明       |
|--------------------|------|----------|
| `range`            | Object | 时间范围   |
| `chatMessageCount` | Long | 私信消息数   |
| `lobbyMessageCount` | Long | 大厅消息数  |
| `groupCount`       | Long | 群组数量    |
| `forumPostCount`   | Long | 论坛发帖数，排除已删除帖子 |
| `forumReplyCount`  | Long | 论坛回复数，排除已删除回复 |
| `hotSections`      | Array | 热门版块 Top 5，按发帖数+回复数排序 |
| `hotSections[].sectionId` | Long | 版块 ID |
| `hotSections[].sectionName` | String | 版块名称 |
| `hotSections[].postCount` | Long | 版块发帖数 |
| `hotSections[].replyCount` | Long | 版块回复数 |
| `hotSections[].hotValue` | Long | 热度值，发帖数+回复数 |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "range": {
      "rangeType": "month",
      "startTime": "2026-04-01T00:00:00",
      "endTime": "2026-05-01T00:00:00"
    },
    "chatMessageCount": 1560,
    "lobbyMessageCount": 432,
    "groupCount": 28,
    "forumPostCount": 86,
    "forumReplyCount": 214,
    "hotSections": [
      {
        "sectionId": 3,
        "sectionName": "综合讨论",
        "postCount": 32,
        "replyCount": 98,
        "hotValue": 130
      }
    ]
  }
}
```

### 4.4 AI 调用统计

- 请求：`GET /api/sys/dashboard/ai`
- 查询参数：公共查询参数
- 响应：`DashboardAiVO`

| 字段                  | 类型   | 说明       |
|---------------------|------|----------|
| `range`             | Object | 时间范围   |
| `aiCallCount`       | Long | AI 调用总次数 |
| `aiSuccessCallCount` | Long | 成功调用次数  |
| `aiFailedCallCount` | Long | 失败调用次数  |
| `ragCallCount`      | Long | RAG 调用次数，当前按 `ai_usage_log.request_scene_type=rag` 统计 |
| `agentTaskCount`    | Long | Agent 任务总数 |
| `agentSuccessTaskCount` | Long | Agent 成功任务数，状态 `2` |
| `agentFailedTaskCount` | Long | Agent 失败任务数，状态 `3` |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "range": {
      "rangeType": "today",
      "startTime": "2026-05-03T00:00:00",
      "endTime": "2026-05-04T00:00:00"
    },
    "aiCallCount": 68,
    "aiSuccessCallCount": 65,
    "aiFailedCallCount": 3,
    "ragCallCount": 5,
    "agentTaskCount": 12,
    "agentSuccessTaskCount": 9,
    "agentFailedTaskCount": 2
  }
}
```

- 说明：
    - RAG 命中数、无命中数待 RAG 主链路和命中日志字段落地后补充。

### 4.5 治理统计

- 请求：`GET /api/sys/dashboard/governance`
- 查询参数：公共查询参数
- 响应：`DashboardGovernanceVO`

| 字段                    | 类型   | 说明                    |
|-----------------------|------|-----------------------|
| `range`               | Object | 时间范围              |
| `reportCount`         | Long | 举报总数（时间范围内）       |
| `pendingReportCount`  | Long | 待处理举报数（全局，不受时间范围限制） |
| `processingReportCount` | Long | 处理中举报数（时间范围内）   |
| `handledReportCount`  | Long | 已处理举报数（时间范围内）    |
| `rejectedReportCount` | Long | 已驳回举报数（时间范围内）    |
| `averageHandleDurationMinutes` | Decimal | 平均举报处理耗时，单位分钟，按 `handled_at - reported_at` 计算 |
| `punishmentDistributions` | Array | 处罚类型分布，空处罚类型归为 `none` |
| `punishmentDistributions[].punishmentType` | String | 处罚类型 |
| `punishmentDistributions[].count` | Long | 数量 |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "range": {
      "rangeType": "all",
      "startTime": null,
      "endTime": null
    },
    "reportCount": 35,
    "pendingReportCount": 1,
    "processingReportCount": 2,
    "handledReportCount": 28,
    "rejectedReportCount": 4,
    "averageHandleDurationMinutes": 18.75,
    "punishmentDistributions": [
      {
        "punishmentType": "mute",
        "count": 10
      },
      {
        "punishmentType": "none",
        "count": 6
      }
    ]
  }
}
```

- 说明：
    - `pendingReportCount` 始终为全局待处理数量，不受时间范围筛选，用于后台侧边栏红点提醒。
    - 其余字段均按请求中的时间范围统计。

### 4.6 导出运营看板统计

- 请求：`GET /api/sys/dashboard/export`
- 查询参数：公共查询参数
- 鉴权：`sys:dashboard:query`
- 响应：Excel 文件流

响应头：

| 响应头 | 说明 |
|------|------|
| `Content-Type` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| `Content-Disposition` | 附件下载，文件名格式 `dashboard-yyyy-MM-dd.xlsx` |

导出内容：

- `概览`、`内容`、`社区`、`AI`、`治理` 汇总 sheet。
- `热门版块` 明细 sheet。
- `处罚分布` 明细 sheet。

边界说明：

- 时间范围校验规则与其他看板接口一致。
- 导出统计口径与对应 JSON 看板接口一致。
