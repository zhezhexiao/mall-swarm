package com.macro.mall.ai.conversation;

import com.macro.mall.ai.model.Message;
import com.macro.mall.ai.model.RoutingHints;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    private String conversationId;
    private String userId;

    @Builder.Default
    private List<Message> history = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> toolStates = new HashMap<>();

    @Builder.Default
    private RoutingHints routingHints = RoutingHints.builder().build();

    @Builder.Default
    private Map<String, String> userFacts = new HashMap<>();

    private String traceId;
    private String modelUsed;
    private int totalTokens;
    private Instant createdAt;
    private Instant lastActiveAt;

    private boolean terminated;
    private int stepCount;
}
