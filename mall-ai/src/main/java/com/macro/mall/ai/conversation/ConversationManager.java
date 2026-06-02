package com.macro.mall.ai.conversation;

import com.macro.mall.ai.config.ConversationProperties;
import com.macro.mall.ai.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ConversationManager {

    private final ConversationStore store;
    private final ConversationProperties props;

    public ConversationManager(ConversationStore store, ConversationProperties props) {
        this.store = store;
        this.props = props;
    }

    /**
     * 统一入口：加载或创建会话。
     */
    public ConversationContext loadOrCreate(String conversationId, String userId) {
        // 1. 尝试加载已存在会话
        if (conversationId != null) {
            ConversationContext existing = store.load(conversationId);
            if (existing != null) {
                existing.setLastActiveAt(Instant.now());
                return existing;
            }
        }

        // 2. 创建新会话
        ConversationContext ctx = ConversationContext.builder()
                .conversationId(UUID.randomUUID().toString())
                .userId(userId)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .traceId(UUID.randomUUID().toString())
                .build();

        log.debug("Created new conversation: {}", ctx.getConversationId());
        return ctx;
    }

    /**
     * 持久化并裁剪历史。
     */
    public void save(ConversationContext ctx) {
        ctx.setLastActiveAt(Instant.now());
        trimHistory(ctx);
        store.save(ctx);
    }

    /**
     * 裁剪历史：只保留最近 N 轮（一轮 = user + assistant）。
     * 保留前 2 轮作为长期上下文锚点。
     */
    private void trimHistory(ConversationContext ctx) {
        List<Message> history = ctx.getHistory();
        int maxRounds = props.getMaxHistoryRounds();
        int maxMessages = maxRounds * 2; // user + assistant per round

        if (history.size() > maxMessages) {
            // 保留前 2 条（首次 user + assistant）作为锚点
            int anchorSize = Math.min(4, history.size() - maxMessages);
            List<Message> anchors = history.subList(0, anchorSize);
            List<Message> recent = history.subList(history.size() - maxMessages, history.size());

            history.clear();
            history.addAll(anchors);
            history.addAll(recent);

            log.debug("Trimmed history: {} -> {} messages", 
                    anchors.size() + recent.size() + (history.size() - anchors.size() - recent.size()),
                    history.size());
        }
    }
}
