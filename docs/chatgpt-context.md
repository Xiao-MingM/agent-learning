# ChatGPT 代码导师项目上下文

> 本文档用于让 ChatGPT 或其他代码导师快速理解当前项目，并在不引入不必要复杂度的前提下继续指导学习。
>
> 项目目标：使用 Java 21、Spring Boot、Maven 和 Spring AI，实现一个最小可运行的 Tool Calling Agent。

## 1. 项目概览

- 项目名称：`tool-calling-agent`
- Java：21
- Spring Boot：4.0.8
- Spring AI：2.0.0
- 构建工具：Maven
- 大模型提供方：OpenAI 兼容的 Spring AI Chat Model
- 当前模型：`gpt-4.1-mini`
- 当前场景：用户输入订单号或订单问题，由大模型决定调用订单查询工具，再根据工具结果生成自然语言回答。

当前架构：

```text
Controller
    -> AgentService
        -> ChatClient
            -> LLM
                -> @Tool
                    -> OrderService
                <- Tool Result
            <- LLM 总结
        <- 最终文本
    <- HTTP JSON 响应
```

这是一个“由 Spring AI 管理工具调用循环”的最小 Agent，并没有自行实现 Agent Loop、任务规划器或多 Agent 框架。

## 2. 完整目录树

下面是当前源码仓库的完整项目树，包含本文档本身；不展开 `.git/`、`.idea/`、`target/` 等版本控制元数据、IDE 配置和构建产物。

```text
agent-learning/
├── .gitignore
├── AGENT.md
├── README.md
├── pom.xml
├── docs/
│   ├── chatgpt-context.md
│   └── java21-basics.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── example/
    │   │           └── agent/
    │   │               ├── AgentLearningApplication.java
    │   │               ├── controller/
    │   │               │   └── AgentController.java
    │   │               ├── domain/
    │   │               │   └── Order.java
    │   │               ├── service/
    │   │               │   ├── AgentService.java
    │   │               │   └── OrderService.java
    │   │               └── tool/
    │   │                   └── OrderTools.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/
            └── com/
                └── example/
                    └── agent/
                        ├── AgentLearningApplicationTest.java
                        ├── service/
                        │   └── OrderServiceTest.java
                        └── tool/
                            └── OrderToolsTest.java
```

本地可能存在但不属于源码树的内容：

- `.git/`：Git 仓库数据。
- `.idea/`：IntelliJ IDEA 本地配置。
- `target/`：Maven 编译和测试产物。
- `.m2/`：本地 Maven 仓库或缓存。
- `fix-java-env.ps1`：本机 Java 环境修复脚本，已被忽略，不属于项目业务代码。

## 3. `pom.xml` 关键配置和依赖

### 3.1 版本配置

| 配置 | 当前值 | 作用 |
| --- | --- | --- |
| Spring Boot Parent | `4.0.8` | 统一管理 Spring Boot 依赖和插件版本 |
| Java | `21` | 指定源码和字节码使用 Java 21 |
| Spring AI BOM | `2.0.0` | 统一管理 Spring AI 模块版本 |

### 3.2 关键依赖

| Maven 坐标 | Scope | 作用 |
| --- | --- | --- |
| `org.springframework.boot:spring-boot-starter-web` | compile | 提供 Spring MVC、HTTP Controller 和内嵌 Web 服务器 |
| `org.springframework.ai:spring-ai-starter-model-openai` | compile | 自动配置 OpenAI Chat Model、`ChatClient.Builder` 和 Tool Calling 能力 |
| `org.springframework.boot:spring-boot-starter-test` | test | 提供 JUnit、Spring Test 和常用测试支持 |

### 3.3 构建插件

| 插件 | 作用 |
| --- | --- |
| `org.springframework.boot:spring-boot-maven-plugin` | 打包和运行 Spring Boot 应用 |

关键 XML 摘要：

```xml
<properties>
    <java.version>21</java.version>
    <spring-ai.version>2.0.0</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 4. 运行配置

`src/main/resources/application.yml` 的作用：

```yaml
spring:
  application:
    name: tool-calling-agent
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4.1-mini
```

- API Key 从环境变量 `OPENAI_API_KEY` 读取，避免把密钥提交进 Git。
- `model` 决定实际调用的大模型。
- Spring AI 根据这些配置创建底层 `ChatModel` 和可注入的 `ChatClient.Builder`。
- 当前没有配置对话 Memory、超时、重试、Token 上限或流式响应。

## 5. 所有 Java 类型

Java 21 的 `record` 也是 Java 类型。为保证清单完整，下面同时列出顶层类、嵌套 `record` 和测试类。

### 5.1 生产代码

| 类型名 | 包路径 | 类型 | 职责 |
| --- | --- | --- | --- |
| `AgentLearningApplication` | `com.example.agent` | class | Spring Boot 启动入口，创建 Spring ApplicationContext 并启动 Web 应用。 |
| `AgentController` | `com.example.agent.controller` | class | HTTP 接口层，接收用户问题、调用 `AgentService`，并返回最终答案。 |
| `AgentController.ChatRequest` | `com.example.agent.controller` | nested record | 表示 `/api/agent/chat` 的请求体，保存用户的 `question`。 |
| `AgentController.ChatResponse` | `com.example.agent.controller` | nested record | 表示接口响应体，保存大模型最终生成的 `answer`。 |
| `Order` | `com.example.agent.domain` | record | 订单领域数据，包含订单编号、商品名称和订单状态。 |
| `AgentService` | `com.example.agent.service` | class | 最小 Agent 的编排中心：构造 Prompt、注册 Tool、调用 LLM，并取得最终答案。 |
| `OrderService` | `com.example.agent.service` | class | 模拟订单业务系统，根据订单编号查询内存中的订单数据。 |
| `OrderTools` | `com.example.agent.tool` | class | 向大模型暴露订单查询工具，把模型的工具调用转换为 `OrderService` 查询。 |
| `OrderTools.OrderQueryResult` | `com.example.agent.tool` | nested record | Tool 的结构化返回值，包含是否找到、说明消息和订单数据。 |

### 5.2 测试代码

| 类名 | 包路径 | 职责 |
| --- | --- | --- |
| `AgentLearningApplicationTest` | `com.example.agent` | 验证 Spring Boot ApplicationContext 能成功启动和装配。测试使用假 API Key，不实际请求 LLM。 |
| `OrderServiceTest` | `com.example.agent.service` | 验证已存在和不存在的订单编号都能得到预期查询结果。 |
| `OrderToolsTest` | `com.example.agent.tool` | 直接调用 Tool 方法，验证成功和失败时的结构化 Tool Result。 |

## 6. Agent 概念说明

| 概念 | 在本项目中的含义 |
| --- | --- |
| LLM | 负责理解用户意图、决定是否调用工具，并根据工具结果生成最终回答的大模型。 |
| Prompt | 发送给 LLM 的指令和用户问题，包括 system prompt 与 user prompt。 |
| Tool | LLM 可选择调用的 Java 方法；本项目中是 `queryOrder`。 |
| Context | 模型本次推理能看到的信息，包括系统指令、用户问题、工具定义和 Tool Result。 |
| Runtime | 负责启动应用、调用模型、执行工具并维持一次工具调用循环的运行环境。 |
| Memory | 跨轮次保留历史对话或状态的机制；当前项目尚未实现。 |

## 7. 每个生产类型对应的 Agent 概念

标记说明：`●` 表示主要职责，`○` 表示间接参与，`—` 表示不承担该概念。

| Java 类型 | LLM | Prompt | Tool | Context | Runtime | Memory | 说明 |
| --- | :---: | :---: | :---: | :---: | :---: | :---: | --- |
| `AgentLearningApplication` | ○ | — | — | ○ | ● | — | 启动 Spring 容器；LLM 和 Tool 相关 Bean 由容器装配。 |
| `AgentController` | — | — | — | ● | ○ | — | 用户输入进入 Agent Context 的 HTTP 边界。 |
| `AgentController.ChatRequest` | — | ○ | — | ● | — | — | 承载用户问题，之后成为 user prompt。 |
| `AgentController.ChatResponse` | ○ | — | — | ○ | — | — | 承载 LLM 最终生成的文本。 |
| `Order` | — | — | ○ | ● | — | — | Tool 返回给模型的结构化业务上下文。 |
| `AgentService` | ● | ● | ● | ● | ● | — | 连接所有核心概念，是当前最小 Agent 的编排中心。 |
| `OrderService` | — | — | ○ | ● | ○ | — | Tool 背后的业务数据来源，不直接暴露给 LLM。 |
| `OrderTools` | — | — | ● | ● | ○ | — | `@Tool` 方法及其描述会暴露给 LLM，运行时由 Spring AI 调用。 |
| `OrderTools.OrderQueryResult` | — | — | ● | ● | — | — | Tool 的结构化结果，会加入本轮模型上下文。 |

需要特别注意：

- 项目中的 LLM 实例并不是手写 Java 类，而是由 `spring-ai-starter-model-openai` 根据配置自动创建。
- `AgentService` 不等同于 LLM，它是调用和编排 LLM 的应用服务。
- `@Tool` 的 `description` 属于工具元数据。它会作为可用工具定义提供给模型，但不是业务代码里单独维护的 system prompt。
- 当前所有类型的 `Memory` 都是 `—`，因为项目没有使用 `ChatMemory`，也没有保存历史消息。

### 7.1 测试类对应关系

测试类不是生产时 Agent 调用链的一部分，它们验证对应概念是否正确实现：

| 测试类 | 主要验证的 Agent 概念 |
| --- | --- |
| `AgentLearningApplicationTest` | Runtime、Spring Context 装配 |
| `OrderServiceTest` | Tool 背后的业务数据和 Context 来源 |
| `OrderToolsTest` | Tool 执行与 Tool Result |

当前测试没有验证真实 LLM 调用、模型是否选择 Tool、Tool Result 是否被再次发给模型，也没有验证 Memory。

## 8. Tool Calling 完整调用链

### 8.1 正常调用顺序

1. 客户端向 `POST /api/agent/chat` 发送 JSON：

   ```json
   {
     "question": "查询订单 A1001 现在是什么状态？"
   }
   ```

2. `AgentController` 将 JSON 反序列化为 `ChatRequest`。
3. `AgentController` 调用 `AgentService.chat(question)`。
4. `AgentService` 通过 `ChatClient` 组装本轮请求：
   - 默认 system prompt：要求模型处理订单事实时必须调用工具、不能猜测。
   - user prompt：用户本次问题。
   - tools：注册 Spring Bean `OrderTools`。
5. `ChatClient` 通过 Spring AI 自动配置的 OpenAI `ChatModel` 把 Prompt 和 Tool 定义发送给 LLM。
6. LLM 识别出需要订单事实，不直接编造答案，而是返回一个 Tool Call，例如：

   ```text
   tool: queryOrder
   arguments: {"orderId":"A1001"}
   ```

7. Spring AI Tool Calling Runtime 识别模型返回的 Tool Call，并找到 `OrderTools.queryOrder`。
8. Spring AI 把模型给出的 JSON 参数转换成 Java `String orderId`，然后调用 `queryOrder("A1001")`。
9. `OrderTools` 调用 `OrderService.findById("A1001")`。
10. `OrderService` 从内存 `Map` 中查找订单，并返回 `Optional<Order>`。
11. `OrderTools` 将查询结果转换为 `OrderQueryResult`：
    - 找到时：`found=true`，包含消息和 `Order`。
    - 未找到时：`found=false`，包含说明消息，`order=null`。
12. Spring AI 将 `OrderQueryResult` 序列化为 Tool Result，并作为新的 Context 加入当前对话。
13. Spring AI 再次调用 LLM。此时模型能看到原问题和真实 Tool Result。
14. LLM 根据 Tool Result 生成自然语言总结，例如“订单 A1001 的商品是机械键盘，当前状态是已发货”。
15. `ChatClient.call().content()` 返回最终文本给 `AgentService`。
16. `AgentController` 将文本包装为 `ChatResponse`，以 HTTP JSON 返回客户端。

### 8.2 简化时序图

```text
Client
  -> AgentController: question
  -> AgentService: chat(question)
  -> ChatClient: system + user + tools
  -> LLM: Prompt + Tool Definition
  <- LLM: Tool Call(queryOrder, orderId)
  -> OrderTools: queryOrder(orderId)
  -> OrderService: findById(orderId)
  <- OrderService: Optional<Order>
  <- OrderTools: OrderQueryResult
  -> LLM: original messages + Tool Result
  <- LLM: final natural-language answer
  <- AgentService: answer
  <- AgentController: ChatResponse
  <- Client: JSON response
```

这里的关键点是：代码只调用了一次 `chatClient.prompt()...call()`，但一次 `call()` 内部可能包含“LLM -> Tool -> LLM”多步交互。这个循环由 Spring AI 的 Tool Calling Runtime 管理，而不是项目手写 `while` 循环。

## 9. 当前 Context 与 Memory 边界

当前一次请求内的 Context 包含：

- system prompt；
- 当前用户问题；
- `OrderTools` 的工具名称、描述和参数定义；
- 工具执行后的 `OrderQueryResult`；
- LLM 在当前调用中的消息。

当前不存在跨请求 Memory。因此：

```text
第一轮：用户说“查询 A1001”     -> 可以回答
第二轮：用户说“它什么时候到？” -> 不知道“它”仍指 A1001
```

除非客户端在第二轮重新提供订单号或完整历史，否则服务端不能自动回忆上一轮内容。这种无状态行为对第一个学习项目是合理的，也更容易理解和测试。

## 10. 当前测试覆盖

| 层级 | 已覆盖 | 未覆盖 |
| --- | --- | --- |
| Spring 启动 | ApplicationContext 能加载 | 真实 API Key 和模型连接 |
| `OrderService` | 存在/不存在订单 | 异常、并发、持久化 |
| `OrderTools` | 成功/失败 Tool Result | Spring AI 对 `@Tool` 的参数转换和真实调用 |
| Agent 调用链 | 无真实端到端测试 | LLM 选择工具、二次模型调用、最终总结 |
| Controller | 无独立接口测试 | 请求校验、HTTP 状态和响应 JSON |
| Memory | 未实现 | 多轮对话 |

## 11. 当前项目不足

这些不足不代表当前实现错误，而是最小学习版本有意省略的能力。

### 11.1 Agent 能力

- 只有一个 Tool，暂时无法观察模型在多个工具之间如何选择。
- 没有 Memory，每次 HTTP 请求都是独立对话。
- 没有任务规划、反思或自主循环；当前属于最小 Tool Calling Agent。
- system prompt 直接写在 `AgentService` 中，内容增长后不易维护。
- 没有限制模型最大输出、工具调用次数或整次请求成本。

### 11.2 工具和业务层

- 订单数据硬编码在内存 `Map`，应用重启后仍只是固定演示数据。
- Tool 参数只依靠模型生成，没有显式校验空订单号或非法格式。
- Tool Result 的未找到场景使用了 `order=null`，结构简单但调用方需要理解可空值。
- 没有模拟业务系统超时、异常或不可用时的处理。

### 11.3 接口和运行保障

- Controller 没有对空白问题做 Bean Validation。
- 没有统一异常处理，模型调用失败时可能直接暴露默认服务器错误。
- 没有鉴权、用户隔离、限流或敏感信息保护，不能直接作为生产接口开放。
- 没有展示 Tool Call、耗时、Token 使用量和模型响应的可观测性。
- 没有流式响应。

### 11.4 测试

- 单元测试覆盖了订单服务和工具结果，但没有覆盖 Controller。
- 没有用 Mock ChatModel 验证完整工具调用链。
- 没有真实模型集成测试；因此不能由自动测试证明模型一定会按 Prompt 调用 Tool。
- 没有多轮 Memory 测试，因为 Memory 尚未实现。

## 12. 下一步升级建议

升级时应一次只引入一个新概念，并在每次修改后运行 `mvn test`。

### 阶段 1：补齐最小版本的可验证性

建议优先完成：

1. 为 `AgentController` 增加简单的空问题校验和接口测试。
2. 增加清晰日志，至少能看到“收到问题、Tool 被调用、订单号、Tool 是否成功”，但不要记录 API Key。
3. 增加一个可手工运行的真实模型验证说明，自动化测试仍使用假 Key，避免测试产生费用。
4. 把模型名称改为可由环境变量覆盖，默认值仍保持简单。

学习目标：理解 HTTP 输入、Agent 编排、Tool 执行和测试边界。

### 阶段 2：增加第二个 Tool

例如增加“查询物流预计到达时间”工具，让模型根据问题选择：

- `queryOrder`：查订单商品和状态；
- `queryDelivery`：查物流和预计到达时间。

学习目标：理解 Tool 描述、参数设计和模型的 Tool Selection。不要为了两个 Tool 引入复杂注册中心。

### 阶段 3：加入最小 Memory

在理解单轮调用后，再使用 Spring AI `ChatMemory`，由客户端提供 `conversationId`，保存有限条历史消息。

学习目标：区分：

- 当前请求数据是 Context；
- 跨请求保留的历史是 Memory；
- Memory 需要会话隔离和容量限制。

### 阶段 4：增强 Tool 的可靠性

- 校验 Tool 参数；
- 把业务异常转换为模型可理解、用户不可误解的 Tool Result；
- 为外部订单接口增加超时和有限重试；
- 明确哪些 Tool 是只读的，哪些 Tool 会修改真实数据；
- 对有副作用的 Tool 增加确认和权限检查。

学习目标：理解 Tool 不只是普通方法，它可能让模型触发真实世界操作。

### 阶段 5：替换演示数据并增加可观测性

- 将内存订单替换为数据库或模拟 HTTP 订单服务；
- 记录模型名称、请求耗时、Tool 名称、Tool 耗时和错误类型；
- 视需要加入指标或链路追踪；
- 增加端到端测试，并将真实模型测试与普通 `mvn test` 分离。

学习目标：理解 Agent Runtime 在真实系统中的稳定性和诊断需求。

### 暂时不建议立即加入

- 多 Agent 协作框架；
- 向量数据库和 RAG；
- 工作流引擎；
- 自定义 Agent Loop；
- 消息队列、微服务拆分和复杂领域分层。

这些能力以后可能有价值，但会遮挡当前最重要的学习主线：Prompt 如何描述任务、LLM 如何选择 Tool、Runtime 如何执行 Tool、Tool Result 如何回到 LLM。

## 13. 给 ChatGPT 代码导师的指导约束

后续指导本项目时，请遵守以下约束：

1. 学习者熟悉 Java 8，但正在适应 Java 21；遇到 `record`、模式匹配、文本块等新语法时先解释再修改。
2. 优先保持 `Controller -> AgentService -> ChatClient -> LLM -> @Tool -> OrderService` 这条主线清楚。
3. 不要为了“架构完整”主动加入复杂接口层、工厂、注册中心、工作流或多 Agent 框架。
4. 每次只引入一个主要 Agent 概念，并说明它对应 LLM、Prompt、Tool、Context、Runtime 或 Memory 中的哪一个。
5. 明确区分 Spring AI 自动管理的 Tool Calling 循环和项目自己编写的业务代码。
6. 当前没有 Memory，不要把 Spring Bean、内存订单 `Map` 或单次请求 Context 误称为 Agent Memory。
7. 每个关键类保持清晰注释，但避免注释重复逐字描述代码。
8. 每次修改后使用项目指定的 Java 21 和 Maven 执行 `mvn test`。
9. 需要真实调用 LLM 时，应先说明需要 `OPENAI_API_KEY`，并提示可能产生网络请求和费用。
10. 不确定 Spring AI 版本 API 时先核实当前 `pom.xml` 的版本，不要直接套用旧版本示例。

## 14. 建议的导师阅读顺序

首次接手项目时，建议按下面顺序阅读：

1. `pom.xml`：确认 Java、Spring Boot 和 Spring AI 版本。
2. `application.yml`：确认模型和 API Key 来源。
3. `AgentService`：理解 Prompt、LLM、Tool 注册和 Agent Runtime 的核心连接点。
4. `OrderTools`：理解 Java 方法如何成为 LLM Tool。
5. `OrderService` 和 `Order`：理解工具背后的业务数据。
6. `AgentController`：理解外部请求如何进入 Agent Context。
7. 三个测试类：理解目前已经验证和尚未验证的边界。

这份项目的核心不是代码数量，而是看清一次 Tool Calling 中信息和控制权的流动：用户提供问题，LLM 决定调用工具，Java Runtime 执行工具，结果回到 LLM，最后由 LLM 组织面向用户的答案。
