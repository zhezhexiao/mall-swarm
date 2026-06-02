package com.macro.mall.ai.agent;

import com.macro.mall.ai.conversation.ConversationContext;
import com.macro.mall.ai.model.ChatResponse;
import com.macro.mall.ai.router.RouteStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Order(Ordered.LOWEST_PRECEDENCE)
@Component("agentExecutor")
@RequiredArgsConstructor
public class AgentEngine implements RouteStrategy {

    private final ExecutionLoop executionLoop;

    @Override
    public boolean matches(ConversationContext ctx) {
        return true; // fallback
    }

    @Override
    public ChatResponse execute(ConversationContext ctx) {
        return executionLoop.execute(ctx);
    }

    @Override
    public Flux<String> executeStream(ConversationContext ctx) {
        return executionLoop.executeStream(ctx);
    }
}
