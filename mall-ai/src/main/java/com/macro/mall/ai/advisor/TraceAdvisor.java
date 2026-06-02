package com.macro.mall.ai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * TraceAdvisor — 将 traceId 注入 advisor 链路。
 *
 * 从 MDC 读取 traceId（由 ChatApplicationService 设置），
 * 写入 adviseContext，供 AuditAdvisor 等下游使用。
 */
@Slf4j
@Component
public class TraceAdvisor implements BaseAdvisor {

    @Override
    public AdvisedRequest before(AdvisedRequest request) {
        String raw = MDC.get("traceId");
        final String traceId = raw != null ? raw : "no-trace";
        log.debug("[{}] TraceAdvisor: request intercepted", traceId);

        return request.updateContext(ctx -> {
            ctx.put("traceId", traceId);
            return ctx;
        });
    }

    @Override
    public AdvisedResponse after(AdvisedResponse response) {
        return response;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 最先执行，确保 traceId 先写入 context
    }
}
