# ChatAttachment 策略工厂重构计划

## Summary

将聊天附件后处理从 `switch(messageType)` 重构为策略工厂。目标只覆盖 `image` 与 `voice` 两类附件后处理，保留现有异步任务、消息回写和指标链路不变。

## Key Changes

- 新增统一策略接口，定义“加载文件、补齐元数据、生成派生资源、返回是否变更”的契约。
- 新增图片/语音具体策略类，分别承接当前 `ChatAttachmentImageProcessor` 和 `ChatAttachmentVoiceProcessor` 的职责。
- 新增策略工厂，根据 `messageType` 或 MIME 类型路由到具体策略。
- 改造 `ChatAttachmentAsyncProcessingServiceImpl`，只负责任务编排，不再写消息类型分支。
- 保留 `ChatAttachmentMetadataResolver` 作为能力组件，语音策略继续复用，避免重复实现。
- 同步更新 `docs/tasks/README.md`，登记该重构任务状态与完成标准。

## Test Plan

- 图片消息：补齐宽高、生成缩略图、更新 payload。
- 语音消息：补齐时长/波形、生成 WAV 预览、更新转码状态。
- 不支持类型：工厂返回空或拒绝处理，主流程跳过。
- 任务失败与重试：策略异常不会破坏原有任务状态流转。
- 回归：消息更新、推送、指标统计保持不变。

## Assumptions

- 仅处理 `image` 与 `voice`，不扩展到通用文件类型。
- 策略按消息类型静态路由，不引入配置化插件扫描。
- 工厂只做选择，不持有业务状态。
