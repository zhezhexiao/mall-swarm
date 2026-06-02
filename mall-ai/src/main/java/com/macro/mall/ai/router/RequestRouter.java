package com.macro.mall.ai.router;

import com.macro.mall.ai.conversation.ConversationContext;
import com.macro.mall.ai.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RequestRouter {

    private final List<RouteStrategy> strategies;

    public ChatResponse route(ConversationContext ctx) {
        for (RouteStrategy strategy : strategies) {
            if (strategy.matches(ctx)) {
                return strategy.execute(ctx);
            }
        }
        throw new IllegalStateException("No route matched");
    }

    /** 流式路由 — FastPath 包装为单元素 Flux，Agent 返回真流。 */
    public Flux<String> routeStream(ConversationContext ctx) {
        for (RouteStrategy strategy : strategies) {
            if (strategy.matches(ctx)) {
                return strategy.executeStream(ctx);
            }
        }
        return Flux.error(new IllegalStateException("No route matched"));
    }
}
