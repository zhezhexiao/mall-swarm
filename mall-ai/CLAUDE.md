# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Module overview

`mall-ai` is the AI Interaction Layer of the `mall-swarm` microservice stack. It is a Spring Boot 3.5 / Spring Cloud 2025 / Spring AI 1.0.0-M6 service that exposes a chat endpoint backed by DeepSeek (via the OpenAI-compatible API), with a tool-calling agent that can search products (mall-search) and query orders (mall-admin).

- Service port: **8002**, registered to Nacos as `mall-ai`
- Auth: Sa-Token (type `memberLogin`, shares session with `mall-portal`)
- Storage: Redis (db1 for conversation state, db0 for Sa-Token session)
- Gateway route: `mall-gateway` matches `/mall-ai/**` and applies `StpMemberUtil.checkLogin()`

## Build & run

This is a sub-module of the `mall-swarm` Maven reactor. Build from the module directory or the parent.

```bash
# Compile + package this module (and dependencies)
mvn -pl mall-ai -am package -DskipTests

# Run locally (uses application.yml, profile=dev)
mvn -pl mall-ai spring-boot:run

# Run with the prod profile
mvn -pl mall-ai spring-boot:run -Dspring-boot.run.profiles=prod
```

`<skipTests>true</skipTests>` is set in the parent pom, so tests are skipped by default. The module currently has no `src/test` tree.

### `.env` loading

`MallAiApplication.loadDotenv()` reads a `.env` file from the working directory (or `mall-ai/.env`) and sets every `KEY=VALUE` line as a System Property at JVM startup. The most important variable is `DEEPSEEK_API_KEY` (default in `application.yml` is `sk-unset` if the env var is missing). `.env` is git-ignored — copy or create one before running.

## Architecture

### Request flow

```
ChatController (/ai/chat, /ai/chat/stream)
  → ChatApplicationService  (loads/saves ConversationContext, manages traceId)
    → RequestRouter         (iterates RouteStrategy beans in @Order)
      ├─ FastExecutor        (HIGHEST_PRECEDENCE — keyword match greetings)
      │   └─ GreetingHandler
      └─ AgentEngine         (LOWEST_PRECEDENCE — fallback)
          └─ ExecutionLoop
              └─ Planner
                  └─ AiGateway (DeepSeekAiGateway — calls Spring AI ChatClient)
                       ├─ tools: ProductTool, OrderTool
                       └─ advisors: TraceAdvisor, AuditAdvisor, MemoryAdvisor
```

Both routes converge on `ChatResponse` (sync) or a `Flux<String>` (SSE stream). The agent path returns the deepseek model output and a "route" tag (`"fastpath"` or `"agent"`) so callers can tell which path served the request.

### Conversation state

`ConversationContext` is JSON-serialized into Redis under `mall:ai:conv:{conversationId}` with a 30-minute TTL (configurable via `mall-ai.conversation.ttl`). `ConversationManager` trims history to `maxHistoryRounds` user/assistant pairs on save, preserving the first 2 messages as anchor context.

### Tools and `SanitizedToolCallbackWrapper`

`ProductTool` and `OrderTool` are Spring beans with `@Tool`-annotated methods. The `SanitizedToolBeansConfig` wraps them in a sanitizer that strips trailing commas and repairs malformed JSON emitted by DeepSeek before invocation. The wrapped callbacks are registered as a `List<FunctionCallback>` bean and injected into `DeepSeekAiGateway`.

`ProductTool` additionally holds an `AtomicReference<List<ProductItem>>` (static) that the controller uses to emit a structured `[PRODUCTS]json` SSE event after the text stream completes — so the frontend can render product cards without re-parsing LLM prose.

### Auth flow (SSE caveat)

`SaTokenConfig` registers `SaInterceptor` on `/ai/**` BUT excludes `/ai/chat/stream`. Tomcat async dispatch loses the Sa-Token ThreadLocal, so the SSE endpoint manually reads the `Authorization: Bearer …` header in `ChatController.resolveUserId()` and resolves the userId from the JWT. Non-SSE endpoints rely on the interceptor; SSE relies on the Gateway + manual header parsing.

## Quirks worth knowing

- **Hardcoded Redis host for Sa-Token**: `MallAiSaTokenDao` creates its own `LettuceConnectionFactory` against `redis:6379` (no DB number — defaults to db0). It does NOT read `spring.data.redis.*` config. If you change the Redis host, update this class.
- **HTTP/1.1 forced**: `HttpConfig.restClientBuilder()` wires a JDK `HttpClient` pinned to HTTP/1.1 with a permissive trust manager. DeepSeek's API returns `RST_STREAM` over HTTP/2, which Spring's default client would use.
- **`application-prod.yml` uses container hostnames** (`nacos-registry:8848`, `redis`, `mall-search:8081`, `mall-admin:8080`). Dev profile uses `localhost` + the `.env` API key.
- **Tool param `Object[] toolBeans` is unused** in `AiGateway`/`DeepSeekAiGateway` — tools come from the `List<FunctionCallback>` bean wired in `SanitizedToolBeansConfig`.
- **`DeepSeekClient.java` is a stub** kept to avoid Maven incremental-compile cache issues. Do not delete without a clean build.
- **Nacos discovery config**: `fail-fast: false` in dev so the service starts even without a Nacos server.

## Extending the agent

- New tool: implement as a `@Component` with `@Tool` methods, add it to the `toolObjects(...)` list in `SanitizedToolBeansConfig#sanitizedToolCallbacks`. The sanitizer + tool-calling loop is automatic.
- New route (e.g. FAQ RAG): implement `RouteStrategy` and annotate with `@Order` to slot it between `FastExecutor` and `AgentEngine`. `AgentEngine.matches()` returns `true` (fallback) — anything you add must have a higher precedence.
- New advisor: implement `BaseAdvisor`, list it in `Planner` next to the existing three. Use `Ordered.HIGHEST_PRECEDENCE` if it must run before `TraceAdvisor` (which writes `traceId` into the advise context that everyone else reads).
- New model provider: implement `AiGateway` and replace `DeepSeekAiGateway` (only one is wired — there is no multi-model `ModelRouter` yet, that interface is a stub).

## Role behavior guidelines

When working on architecture discussion, code review, or system analysis within this module, adopt the **Senior Java Architect** role:

- Act as a research partner, not an answer generator. Prioritize collaborative analysis over delivering complete outputs.
- Follow incremental research: observe → question → analyze → understand → document.
- Analyze design motivation first before diving into code: *why does it exist? → what problem does it solve? → where does it fit in the system? → upstream/downstream relationships?*
- Never analyze classes in isolation. Always cover upstream/downstream, lifecycle, data flow, control flow. Use ASCII diagrams where helpful.
- Proactively identify Design Patterns and their trade-offs. Focus on **responsibility boundary** — flag any Responsibility Leakage.
- Think like an Architecture Reviewer: answer *"why designed this way?"* rather than *"this class is for..."*.
- Use Chinese as the primary language. For technical terms, use `English（中文翻译）` format, e.g., `Strategy Pattern（策略模式）`.
- Knowledge documentation follows **on-demand mode**: discuss by default; only output Markdown when explicitly requested ("整理一下", "形成文稿", "沉淀成笔记").
