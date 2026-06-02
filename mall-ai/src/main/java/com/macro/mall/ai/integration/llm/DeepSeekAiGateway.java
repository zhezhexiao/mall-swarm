package com.macro.mall.ai.integration.llm;

import com.macro.mall.ai.llm.AiGateway;
import com.macro.mall.ai.model.ChatResponse;
import com.macro.mall.ai.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DeepSeekAiGateway implements AiGateway {

    private final ChatClient.Builder chatClientBuilder;
    private final List<FunctionCallback> sanitizedToolCallbacks;

    public DeepSeekAiGateway(ChatClient.Builder chatClientBuilder,
                             List<FunctionCallback> sanitizedToolCallbacks) {
        this.chatClientBuilder = chatClientBuilder;
        this.sanitizedToolCallbacks = sanitizedToolCallbacks;
    }

    @Override
    public ChatResponse chat(List<Message> messages, Object[] toolBeans, List<Advisor> advisors) {
        ChatClient.ChatClientRequestSpec spec = buildSpec(messages, advisors);
        String reply = spec.call().content();
        log.debug("DeepSeek reply: {} chars", reply.length());

        return ChatResponse.builder()
                .reply(reply)
                .tokenUsage(0)
                .route("agent")
                .build();
    }

    @Override
    public Flux<String> chatStream(List<Message> messages, Object[] toolBeans, List<Advisor> advisors) {
        ChatClient.ChatClientRequestSpec spec = buildSpec(messages, advisors);

        return spec.stream().chatResponse()
                .map(r -> {
                    if (r.getResult() != null && r.getResult().getOutput() != null) {
                        return r.getResult().getOutput().getText();
                    }
                    return "";
                })
                .filter(s -> !s.isEmpty())
                .doOnComplete(() -> log.debug("DeepSeek stream complete"))
                .doOnError(e -> log.error("DeepSeek stream error", e));
    }

    private ChatClient.ChatClientRequestSpec buildSpec(List<Message> messages, List<Advisor> advisors) {
        List<org.springframework.ai.chat.messages.Message> aiMessages = new ArrayList<>();
        for (Message msg : messages) {
            aiMessages.add(switch (msg.getRole()) {
                case "system" -> new org.springframework.ai.chat.messages.SystemMessage(msg.getContent());
                case "user" -> new org.springframework.ai.chat.messages.UserMessage(msg.getContent());
                case "assistant" -> new org.springframework.ai.chat.messages.AssistantMessage(msg.getContent());
                default -> new org.springframework.ai.chat.messages.UserMessage(msg.getContent());
            });
        }

        ChatClient.ChatClientRequestSpec spec = chatClientBuilder.build().prompt()
                .messages(aiMessages.toArray(new org.springframework.ai.chat.messages.Message[0]));

        if (advisors != null && !advisors.isEmpty()) {
            spec.advisors(advisors);
        }

        if (!sanitizedToolCallbacks.isEmpty()) {
            spec.tools(sanitizedToolCallbacks.toArray(new FunctionCallback[0]));
        }

        return spec;
    }
}
