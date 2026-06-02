package com.macro.mall.ai.agent;

import com.macro.mall.ai.advisor.AuditAdvisor;
import com.macro.mall.ai.advisor.MemoryAdvisor;
import com.macro.mall.ai.advisor.TraceAdvisor;
import com.macro.mall.ai.conversation.ConversationContext;
import com.macro.mall.ai.llm.AiGateway;
import com.macro.mall.ai.model.ChatResponse;
import com.macro.mall.ai.model.Message;
import com.macro.mall.ai.tool.order.OrderTool;
import com.macro.mall.ai.tool.product.ProductTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class Planner {

    private final AiGateway aiGateway;
    private final PromptManager promptManager;
    private final TraceAdvisor traceAdvisor;
    private final AuditAdvisor auditAdvisor;
    private final MemoryAdvisor memoryAdvisor;
    private final ProductTool productTool;
    private final OrderTool orderTool;

    public String plan(ConversationContext ctx) {
        List<Message> messages = buildMessages(ctx);
        List<Advisor> advisors = List.of(traceAdvisor, auditAdvisor, memoryAdvisor);
        Object[] tools = { productTool, orderTool };

        log.debug("[{}] Planner: {} messages + {} tools → AiGateway",
                ctx.getTraceId(), messages.size(), tools.length);

        ChatResponse response = aiGateway.chat(messages, tools, advisors);
        log.debug("[{}] Planner: reply {} chars", ctx.getTraceId(), response.getReply().length());
        return response.getReply();
    }

    /** SSE 流式规划 — 逐 token 返回。 */
    public Flux<String> planStream(ConversationContext ctx) {
        List<Message> messages = buildMessages(ctx);
        List<Advisor> advisors = List.of(traceAdvisor, auditAdvisor, memoryAdvisor);
        Object[] tools = { productTool, orderTool };

        log.debug("[{}] Planner stream: {} messages + {} tools",
                ctx.getTraceId(), messages.size(), tools.length);

        return aiGateway.chatStream(messages, tools, advisors);
    }

    private List<Message> buildMessages(ConversationContext ctx) {
        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role("system")
                .content(promptManager.getSystemPrompt(ctx.getUserId()))
                .build());
        messages.addAll(ctx.getHistory());
        return messages;
    }
}
