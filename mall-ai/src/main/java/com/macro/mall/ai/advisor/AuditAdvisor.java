package com.macro.mall.ai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Component;

/**
 * AuditAdvisor — 记录每次 LLM 调用的 latency / tokens。
 *
 * before() 通过 MDC 存 startTime，after() 从 AdvisedResponse.response() 取 usage。
 * traceId 从 adviseContext 读取（由 TraceAdvisor 写入）。
 */
@Slf4j
@Component
public class AuditAdvisor implements BaseAdvisor {

    @Override
    public AdvisedRequest before(AdvisedRequest request) {
        MDC.put("audit.startTime", String.valueOf(System.currentTimeMillis()));
        String traceId = (String) request.adviseContext().getOrDefault("traceId", "N/A");
        log.info("AUDIT [{}] → prompt", traceId);
        return request;
    }

    @Override
    public AdvisedResponse after(AdvisedResponse response) {
        String startStr = MDC.get("audit.startTime");
        long startTime = startStr != null ? Long.parseLong(startStr) : 0;
        long latency = startTime > 0 ? System.currentTimeMillis() - startTime : -1;
        String traceId = (String) response.adviseContext().getOrDefault("traceId", "N/A");

        var aiResponse = response.response();
        if (aiResponse != null && aiResponse.getMetadata() != null) {
            var usage = aiResponse.getMetadata().getUsage();
            if (usage != null) {
                log.info("AUDIT [{}] ← latency: {}ms | tokens: in={} out={} total={}",
                        traceId, latency,
                        usage.getPromptTokens(), usage.getGenerationTokens(), usage.getTotalTokens());
            } else {
                log.info("AUDIT [{}] ← latency: {}ms | tokens: n/a", traceId, latency);
            }
        } else {
            log.info("AUDIT [{}] ← latency: {}ms", traceId, latency);
        }

        return response;
    }

    @Override
    public int getOrder() {
        return 10; // TraceAdvisor 之后执行
    }
}
