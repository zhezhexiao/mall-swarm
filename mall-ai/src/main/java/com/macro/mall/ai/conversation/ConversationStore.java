package com.macro.mall.ai.conversation;

public interface ConversationStore {
    ConversationContext load(String conversationId);
    void save(ConversationContext ctx);
    void expire(String conversationId);
}
