# AI 发送链路责任链重构

## Summary
- 将 `AiChatServiceImpl` 的串行编排抽成内部责任链，`AiChatService` 对外签名不变。
- `sendMessage` 和 `streamMessage` 共用同一套前置 Handler，流式/非流式只在终端执行器上分叉。
- 处理顺序由配置文件默认列表控制，`Builder` 可在测试或未来运行时场景覆盖/重排。

## Key Changes
- 新增 `AiChatHandler`、`AiChatHandlerContext`、`AiChatHandlerChain`、`AiChatChainBuilder`；Handler 返回继续/中断信号，链执行器按顺序短路。
- 拆分现有步骤为独立 Handler：会话校验、渠道加载、额度校验、Token 预算预检、用户消息落库、附件解析、历史上下文裁剪、RAG 检索、模型调用、助手消息落库、额度扣减、日志记录、会话更新时间。
- 增加链顺序配置（如 `ai.chat.chain.send-message.handlers` / `ai.chat.chain.stream-message.handlers`），默认顺序写在配置中，`Builder` 支持按 handler code 重组；未知或重复 code 直接失败。
- `AiChatServiceImpl` 仅保留入口参数处理和链路触发，不再手写步骤顺序。

## Example
```yaml
ai:
  chat:
    chain:
      send-message:
        handlers: [session-validate, channel-load, quota-check, token-budget, user-message, attachments, context, rag, model, assistant-message, quota-record, usage-log, session-touch]
```
```java
AiChatExecutionChain chain = chainBuilder.build(sendMessageOrder);
chain.execute(context);
```

## Test Plan
- Handler 单测：正常继续、条件中断、异常透传。
- Chain 单测：顺序按配置生效、重排生效、缺失/重复 handler 的失败路径。
- Service 回归测：`sendMessage` / `streamMessage` 现有成功、配额失败、附件失败、模型失败路径保持不变。

## Assumptions
- 本次一起覆盖 `sendMessage` 和 `streamMessage`。
- 不改业务逻辑，不改接口文档；仅内部编排重组。
- 配置文件是默认顺序来源，`Builder` 作为覆盖入口。
