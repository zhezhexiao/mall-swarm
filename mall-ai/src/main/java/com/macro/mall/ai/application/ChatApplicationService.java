package com.macro.mall.ai.application;

import com.macro.mall.ai.conversation.ConversationContext;
import com.macro.mall.ai.conversation.ConversationManager;
import com.macro.mall.ai.model.ChatRequest;
import com.macro.mall.ai.model.ChatResponse;
import com.macro.mall.ai.model.Message;
import com.macro.mall.ai.model.ProductItem;
import com.macro.mall.ai.router.RequestRouter;
import com.macro.mall.ai.tool.product.ProductTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatApplicationService {

    private final ConversationManager conversationManager;
    private final RequestRouter requestRouter;
    private final ObjectMapper objectMapper;

    public ChatResponse chat(ChatRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        try {
            ConversationContext ctx = loadCtx(request, traceId);
            ctx.getHistory().add(Message.builder()
                    .role("user").content(request.getMessage())
                    .timestamp(Instant.now()).build());

            ChatResponse response = requestRouter.route(ctx);

            ctx.getHistory().add(Message.builder()
                    .role("assistant").content(response.getReply())
                    .timestamp(Instant.now()).build());

            conversationManager.save(ctx);
            response.setConversationId(ctx.getConversationId());
            return response;
        } finally {
            MDC.clear();
        }
    }

    /**
     * SSE 流式对话。
     * 第一个事件为 "[CID]convId"，后续为 token 文本。
     */
    public Flux<String> chatStream(ChatRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        ConversationContext ctx = loadCtx(request, traceId);
        ctx.getHistory().add(Message.builder()
                .role("user").content(request.getMessage())
                .timestamp(Instant.now()).build());

        StringBuilder fullReply = new StringBuilder();
        String convId = ctx.getConversationId();

        Flux<String> metaEvent = Flux.just("[CID]" + convId);

        Flux<String> textStream = requestRouter.routeStream(ctx)
                .doOnNext(fullReply::append)
                .doOnComplete(() -> {
                    ctx.getHistory().add(Message.builder()
                            .role("assistant").content(fullReply.toString())
                            .timestamp(Instant.now()).build());
                    conversationManager.save(ctx);
                    log.info("[{}] Stream complete: {} chars, conv={}",
                            traceId, fullReply.length(), convId);
                    MDC.clear();
                })
                .doOnError(e -> {
                    log.error("[{}] Stream error", traceId, e);
                    MDC.clear();
                });

        // 文本流结束后，检查是否有结构化商品数据需要直传前端
        Flux<String> productEvent = Mono.fromCallable(() -> {
            List<ProductItem> products = ProductTool.drainLastProducts();
            if (products != null) {
                try {
                    String json = objectMapper.writeValueAsString(products);
                    log.info("[{}] Emitting [PRODUCTS] event: {} items", traceId, products.size());
                    return "[PRODUCTS]" + json;
                } catch (Exception e) {
                    log.warn("[{}] Failed to serialize products", traceId, e);
                }
            }
            return null;
        }).flux().filter(s -> s != null);

        return Flux.concat(metaEvent, textStream, productEvent);
    }

    private ConversationContext loadCtx(ChatRequest request, String traceId) {
        ConversationContext ctx = conversationManager.loadOrCreate(
                request.getConversationId(), request.getUserId());
        // 始终更新 userId（首次可能为 null，后续请求有登录态时补上）
        if (request.getUserId() != null) {
            ctx.setUserId(request.getUserId());
        }
        ctx.setTraceId(traceId);
        log.info("Chat: userId={}, convId={}, msg={}",
                ctx.getUserId(), ctx.getConversationId(), request.getMessage());
        return ctx;
    }
}
