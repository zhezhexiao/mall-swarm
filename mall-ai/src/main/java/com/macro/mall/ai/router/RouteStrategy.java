package com.macro.mall.ai.router;

import com.macro.mall.ai.conversation.ConversationContext;
import com.macro.mall.ai.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface RouteStrategy {
    boolean matches(ConversationContext ctx);
    ChatResponse execute(ConversationContext ctx);

    /** 流式执行，默认包装同步结果。Agent 路径覆写为真流式。 */
    default Flux<String> executeStream(ConversationContext ctx) {
        return Flux.just(execute(ctx).getReply());
    }
}
