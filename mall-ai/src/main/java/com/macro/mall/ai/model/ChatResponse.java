package com.macro.mall.ai.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
    private String reply;
    private String conversationId;
    private String route;      // "fastpath" | "agent"
    private int tokenUsage;
}
