package com.macro.mall.ai.conversation;

import com.macro.mall.ai.config.ConversationProperties;
import com.macro.mall.ai.integration.mysql.MySQLConversationStore;
import com.macro.mall.ai.model.AiConversation;
import com.macro.mall.ai.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ConversationManager {

    private final ConversationStore redisStore;
    private final MySQLConversationStore mysqlStore;
    private final ConversationProperties props;
    private final ChatClient.Builder chatClientBuilder;

    public ConversationManager(ConversationStore redisStore,
                               MySQLConversationStore mysqlStore,
                               ConversationProperties props,
                               ChatClient.Builder chatClientBuilder) {
        this.redisStore = redisStore;
        this.mysqlStore = mysqlStore;
        this.props = props;
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * 统一入口：加载或创建会话。
     * Redis hit → 直接返回
     * Redis miss → MySQL 回源 → 写回 Redis → 返回
     * 都没有 → 新建
     */
    public ConversationContext loadOrCreate(String conversationId, String userId) {
        // 1. 尝试 Redis
        if (conversationId != null) {
            ConversationContext existing = redisStore.load(conversationId);
            if (existing != null) {
                existing.setLastActiveAt(Instant.now());
                return existing;
            }

            // 2. Redis miss → MySQL 回源
            ConversationContext recovered = mysqlStore.loadFromMySQL(conversationId);
            if (recovered != null) {
                recovered.setLastActiveAt(Instant.now());
                redisStore.save(recovered); // 写回 Redis
                log.info("Recovered conversation from MySQL: {}", conversationId);
                return recovered;
            }
        }

        // 3. 创建新会话
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
     * 持久化：Redis(同步) + MySQL(异步)。
     */
    public void save(ConversationContext ctx) {
        ctx.setLastActiveAt(Instant.now());
        trimHistory(ctx);
        redisStore.save(ctx);
        mysqlStore.persistAsync(ctx);
    }

    /**
     * 对话列表。
     */
    public List<AiConversation> listByUserId(Long userId, int page, int size) {
        return mysqlStore.listByUserId(userId, size, page * size);
    }

    /**
     * 软删。
     */
    public void delete(String convId, Long userId) {
        redisStore.expire(convId); // 删 Redis key
        mysqlStore.softDelete(convId, userId);
    }

    /**
     * 重命名。
     */
    public void rename(String convId, String title) {
        mysqlStore.updateTitle(convId, title);
    }

    /**
     * 将匿名对话归属到真实用户。
     */
    public int claimConversations(long realUserId) {
        return mysqlStore.claimConversations(realUserId);
    }

    /**
     * 异步生成对话标题（首轮结束后调用）。
     * 用 DeepSeek 生成 ≤15 字的中文摘要。
     */
    @Async
    public void generateTitleAsync(String convId, String userMsg, String assistantMsg) {
        try {
            String prompt = "请用一句中文概括这段对话的主题，不超过15个字，不要标点符号。\n"
                    + "用户：" + userMsg.substring(0, Math.min(userMsg.length(), 100)) + "\n"
                    + "助手：" + assistantMsg.substring(0, Math.min(assistantMsg.length(), 200));
            String title = chatClientBuilder.build()
                    .prompt(prompt)
                    .call()
                    .content();
            if (title != null) {
                title = title.trim().replaceAll("[。！？,.]", "");
                if (title.length() > 15) title = title.substring(0, 15);
            }
            if (title != null && !title.isEmpty()) {
                mysqlStore.updateTitle(convId, title);
                log.info("Generated title for conv={}: {}", convId, title);
            }
        } catch (Exception e) {
            log.warn("Failed to generate title for conv={}: {}", convId, e.getMessage());
        }
    }

    /**
     * 裁剪历史：只保留最近 N 轮（一轮 = user + assistant）。
     * 保留前 2 轮作为长期上下文锚点。
     */
    private void trimHistory(ConversationContext ctx) {
        List<Message> history = ctx.getHistory();
        int maxRounds = props.getMaxHistoryRounds();
        int maxMessages = maxRounds * 2;

        if (history.size() > maxMessages) {
            int anchorSize = Math.min(4, history.size() - maxMessages);
            List<Message> anchors = history.subList(0, anchorSize);
            List<Message> recent = history.subList(history.size() - maxMessages, history.size());

            history.clear();
            history.addAll(anchors);
            history.addAll(recent);
        }
    }
}
