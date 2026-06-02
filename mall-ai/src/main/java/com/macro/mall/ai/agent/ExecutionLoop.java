package com.macro.mall.ai.agent;

import com.macro.mall.ai.conversation.ConversationContext;
import com.macro.mall.ai.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionLoop {

    private final Planner planner;

    public ChatResponse execute(ConversationContext ctx) {
        log.info("[{}] ExecutionLoop: agent path, {} history messages",
                ctx.getTraceId(), ctx.getHistory().size());

        String reply = planner.plan(ctx);
        log.info("[{}] ExecutionLoop: reply {} chars", ctx.getTraceId(), reply.length());

        return ChatResponse.builder()
                .reply(reply)
                .conversationId(ctx.getConversationId())
                .route("agent")
                .tokenUsage(0)
                .build();
    }

    /** SSE 流式执行，逐 token 返回。 */
    public Flux<String> executeStream(ConversationContext ctx) {
        log.info("[{}] ExecutionLoop stream: {} history messages",
                ctx.getTraceId(), ctx.getHistory().size());
        return planner.planStream(ctx);
    }
}
