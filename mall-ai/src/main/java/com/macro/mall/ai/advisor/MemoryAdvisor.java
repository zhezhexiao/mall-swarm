package com.macro.mall.ai.advisor;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Component;

/**
 * MemoryAdvisor — 只读注入 ConversationContext 历史。
 *
 * Phase1: Planner 直接构建 messages 列表（含历史），故此 Advisor 为透传占位。
 */
@Component
public class MemoryAdvisor implements BaseAdvisor {

    @Override
    public AdvisedRequest before(AdvisedRequest request) {
        return request;
    }

    @Override
    public AdvisedResponse after(AdvisedResponse response) {
        return response;
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
