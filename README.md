# 最小 Tool Calling Agent

这是一个面向学习的 Spring AI 示例，目标是看清楚一次完整的 Tool Calling，而不是搭建生产级 Agent 框架。

## 1. 核心概念

普通聊天只能依赖模型已有知识。Tool Calling 允许模型在回答过程中请求 Java 方法查询真实数据。

- LLM 只生成“调用哪个工具、传什么参数”的请求。
- 真正执行 `OrderService` 的是本地 Java 应用，LLM 不能直接访问业务服务。
- 工具结果会再次发送给 LLM，由 LLM 组织最终回答。
- Spring AI 的 `ChatClient` 自动处理这个循环，本示例不手写 Agent Loop。

## 2. 调用链

```text
HTTP 请求
  -> AgentController
  -> AgentService
  -> ChatClient
  -> LLM 判断需要查询订单
  -> OrderTools.queryOrder(orderId)
  -> OrderService.findById(orderId)
  -> Tool Result 返回给 LLM
  -> LLM 总结
  -> HTTP 响应
```

| 类 | 职责 |
| --- | --- |
| `AgentController` | 接收问题，返回最终回答 |
| `AgentService` | 构造提示词，并把工具交给 `ChatClient` |
| `OrderTools` | 用 `@Tool` 描述 LLM 可以调用的方法 |
| `OrderService` | 查询内存中的订单数据 |
| `Order` | 承载订单数据 |

## 3. 最关键的代码

工具定义：

```java
@Tool(description = "根据订单编号查询订单的商品和当前状态")
public OrderQueryResult queryOrder(
        @ToolParam(description = "订单编号，例如 1001") String orderId) {
    // 调用普通 Java 业务服务
}
```

把工具提供给模型：

```java
return chatClient.prompt()
        .user(question)
        .tools(orderTools)
        .call()
        .content();
```

这里的 `content()` 已经是工具执行后，由 LLM 生成的最终回答。

## 4. 运行项目

需要 JDK 21、Maven 3.6.3 或更高版本，以及 OpenAI API Key。

```powershell
$env:OPENAI_API_KEY = "你的 API Key"
mvn spring-boot:run
```

发送问题：

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/agent/chat `
  -ContentType "application/json" `
  -Body '{"question":"订单 1001 现在是什么状态？"}'
```

预置数据：

- `1001`：机械键盘，已发货
- `1002`：显示器，处理中
- 其他编号：工具返回未找到，由 LLM 向用户说明

## 5. 测试和阅读顺序

```powershell
mvn test
```

单元测试不连接真实 LLM，也不消耗 Token；它们检查业务查询和工具返回结构。完整的 LLM Tool Calling 需要配置 API Key 后通过 HTTP 手动体验。

建议依次阅读 `AgentController`、`AgentService`、`OrderTools`、`OrderService`、`OrderToolsTest`。先看清最小调用链，再学习多工具、对话记忆、人工确认或持久化。

如果你主要熟悉 Java 8，可先阅读 [Java 8 到 Java 21 语法对照](docs/java21-basics.md)。
