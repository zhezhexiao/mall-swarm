# DeepSeekAiGateway 整体设计

> 通过 `/research-author` 跑出的架构研究笔记（Step 2 + Step 3）。
> 范围：设计动机 + 架构位置。代码细节、模式评审留作后续。

## 一、为什么存在

`Planner` 已经依赖 Spring AI 的 `ChatClient`，包一层 `DeepSeekAiGateway` 的理由有三层，按"当下价值"排序：

| # | 理由 | 当下 vs 未来 |
|---|------|--------------|
| 1 | **收敛 ChatClient 的两套 API**：`.call().content()`（同步）和 `.stream().chatResponse()`（SSE）收口到 `chat` / `chatStream` 两个方法 | 当下 |
| 2 | **隐藏消息格式转换**：`buildSpec()` 把领域层 `Message` 翻译成 Spring AI 的 `SystemMessage` / `UserMessage` / `AssistantMessage`，避免在 `Planner` 里重复 | 当下 |
| 3 | **为多模型切换预留 seam**：`AiGateway` 是接口，目前唯一实现是 `DeepSeekAiGateway` | 未来（CLAUDE.md 明确"no `ModelRouter` yet"） |

> 判断：决定它存在的是 (1) + (2)；(3) 是搭头，存在"为未来而设计"的过度抽象风险，待 Step 5 评估。

## 二、系统中位置

属于 **integration layer（集成层）** 的 LLM 适配模块：

```
controller → application service → router → agent → llm-gateway → external LLM
                                                       ↑ 这里
```

| 层次 | 包 | DeepSeekAiGateway 的归属 |
|------|----|--------------------------|
| 表现层 | `controller/` | ❌ |
| 应用层 | `service/` | ❌ |
| 编排层 | `agent/`, `router/` | ❌（被它调用） |
| **集成层** | `integration/llm/` | ✅ |
| 基础设施 | `config/`, `advisor/`, `tool/` | ❌（提供依赖） |

包路径 `com.macro.mall.ai.integration.llm` 的 `integration` 关键字就是定位信号——负责与外部系统（DeepSeek）对话的适配层。

## 三、上下游关系

- **上游（谁调用它）**：只有 `Planner`（`Planner.plan()` / `Planner.planStream()` 各调一次）
- **下游（它依赖谁）**：
  - `ChatClient.Builder` —— Spring AI auto-config 提供，`AiConfig` 是空占位
  - `List<FunctionCallback> sanitizedToolCallbacks` —— `SanitizedToolBeansConfig` 包装 `ProductTool` / `OrderTool` 后注入
- **生命周期**：单例 Bean（`@Component`），启动时构造一次。`sanitizedToolCallbacks` 不可变

## 四、调用链与数据流

```
AgentEngine (@Order LOWEST_PRECEDENCE, fallback)
    └─→ ExecutionLoop                ← 薄包装：加 log + 包 ChatResponse
        └─→ Planner                  ← 构造 messages / advisors / tools 列表
            └─→ DeepSeekAiGateway    ← 翻译消息格式、调 ChatClient
                ├─→ buildSpec()      ← domain Message → Spring AI Message
                ├─→ ChatClient.call() / .stream()
                │       ├─ advisors: TraceAdvisor, AuditAdvisor, MemoryAdvisor
                │       └─ tools:    sanitizedFunctionCallbacks
                └─→ DeepSeek API (HTTP/1.1, see HttpConfig)
```

**控制流**：
- 同步：`Planner.plan()` → `aiGateway.chat()` → `spec.call().content()` → 完整字符串
- 流式：`Planner.planStream()` → `aiGateway.chatStream()` → `spec.stream().chatResponse()` → `Flux<String>`

**数据流（输入 → 输出）**：
- 输入：`List<Message>`、`Object[] toolBeans`（传入但未使用⚠️）、`List<Advisor>`
- 输出（同步）：`ChatResponse { reply, tokenUsage=0, route="agent" }`
- 输出（流式）：`Flux<String>` 逐 token

## 五、待办与遗留问题

### 5.1 `toolBeans` 参数与构造器注入的"两个真相源"

`Planner` 传 `Object[] tools = { productTool, orderTool }` 给 `DeepSeekAiGateway`，但 Gateway 方法体内**完全不用**这个参数；实际使用的是构造器注入的 `sanitizedToolCallbacks`（已经预先包装好的 `FunctionCallback`）。

**接口契约与实际行为不一致**：
- 接口签名：`chat(messages, toolBeans, advisors)` —— 承诺 tool beans 来自调用方
- 实际行为：tools 来自构造器注入，与调用方传入的 `toolBeans` 无关

属于 **Responsibility Leakage / Interface Redundancy** 的设计气味，留给 Step 5 评审。

### 5.2 未来可选深入方向

| 方向 | 步骤 | 价值 |
|------|------|------|
| `buildSpec()` 翻译逻辑 | Step 4 | 改 model / 改 message schema 时必看 |
| 模式识别 + trade-off + 职责边界 | Step 5 | 决定是否重构（去 `toolBeans`？拆 `ModelRouter`？） |
| `SanitizedToolBeansConfig` 的 sanitizer 行为 | 独立主题 | DeepSeek 畸形 JSON 兜底，理解 agent 鲁棒性 |

## 六、研究元信息

- 主题：DeepSeekAiGateway 整体设计
- 范围：Step 2（设计动机）+ Step 3（架构位置与数据流）
- 驱动场景：建立心智模型，无具体诉求
- 关联文件：
  - `src/main/java/com/macro/mall/ai/integration/llm/DeepSeekAiGateway.java`
  - `src/main/java/com/macro/mall/ai/llm/AiGateway.java`
  - `src/main/java/com/macro/mall/ai/agent/Planner.java`
  - `src/main/java/com/macro/mall/ai/agent/ExecutionLoop.java`
  - `src/main/java/com/macro/mall/ai/agent/AgentEngine.java`
  - `src/main/java/com/macro/mall/ai/config/AiConfig.java`
- 顶层参考：`/CLAUDE.md`（Quirks 与 Extending the agent 章节）
