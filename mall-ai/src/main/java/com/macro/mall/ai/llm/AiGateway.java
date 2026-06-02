package com.macro.mall.ai.llm;

import com.macro.mall.ai.model.ChatResponse;
import com.macro.mall.ai.model.Message;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * LLM 网关接口 — 统一封装 ChatClient 调用。
 */
public interface AiGateway {

    ChatResponse chat(
            List<Message> messages,
            Object[] toolBeans,
            List<Advisor> advisors);

    /** SSE 流式调用，逐 token 返回。 */
    Flux<String> chatStream(
            List<Message> messages,
            Object[] toolBeans,
            List<Advisor> advisors);
}
