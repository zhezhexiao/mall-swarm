package com.macro.mall.ai.fastpath;

import com.macro.mall.ai.conversation.ConversationContext;
import com.macro.mall.ai.model.ChatResponse;
import com.macro.mall.ai.router.RouteStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class FastExecutor implements RouteStrategy {

    private final GreetingHandler greetingHandler;

    @Override
    public boolean matches(ConversationContext ctx) {
        String userMessage = getLastUserMessage(ctx);
        if (userMessage == null) return false;
        return greetingHandler.matches(userMessage);
    }

    @Override
    public ChatResponse execute(ConversationContext ctx) {
        String reply = greetingHandler.handle();
        ctx.getRoutingHints().setLastRoute("fastpath");
        return ChatResponse.builder()
                .reply(reply)
                .conversationId(ctx.getConversationId())
                .route("fastpath")
                .build();
    }

    private String getLastUserMessage(ConversationContext ctx) {
        if (ctx.getHistory().isEmpty()) return null;
        return ctx.getHistory().get(ctx.getHistory().size() - 1).getContent();
    }
}
