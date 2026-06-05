package com.macro.mall.ai.integration.mysql;

import com.macro.mall.ai.conversation.ConversationContext;
import com.macro.mall.ai.conversation.ConversationStore;
import com.macro.mall.ai.mapper.AiConversationMapper;
import com.macro.mall.ai.mapper.AiMessageMapper;
import com.macro.mall.ai.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL 冷存储。热路径走 Redis，这里负责：
 * 1. 异步落库（save 被 ConversationManager 调用）
 * 2. Redis miss 时回源（load）
 * 3. 对话列表查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MySQLConversationStore {

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;

    /**
     * 异步落库。ConversationManager.save() 在 Redis 写入后调用此方法。
     */
    @Async
    public void persistAsync(ConversationContext ctx) {
        try {
            persist(ctx);
        } catch (Exception e) {
            log.error("Async persist failed for convId={}", ctx.getConversationId(), e);
        }
    }

    /**
     * 同步落库（用于测试和回源重建）。
     */
    public void persist(ConversationContext ctx) {
        String convId = ctx.getConversationId();
        LocalDateTime now = LocalDateTime.now();

        // 1. upsert conversation
        AiConversation existing = conversationMapper.selectByConvId(convId);
        if (existing == null) {
            // 从 history 第一条用户消息截取 title
            String title = extractTitle(ctx.getHistory());
            long uid = (ctx.getUserId() != null) ? Long.parseLong(ctx.getUserId()) : 0L;
            String claimStatus = (uid == 0L) ? "anonymous" : "claimed";
            AiConversation conv = AiConversation.builder()
                    .convId(convId)
                    .userId(uid)
                    .claimStatus(claimStatus)
                    .title(title)
                    .messageCount(ctx.getHistory().size())
                    .createdAt(toLocalDateTime(ctx.getCreatedAt()))
                    .lastActiveAt(now)
                    .build();
            conversationMapper.insert(conv);
        } else {
            conversationMapper.updateLastActive(convId, now, ctx.getHistory().size());
        }

        // 2. 增量写入消息（只写新消息，避免重复）
        int existingCount = messageMapper.countByConvId(convId);
        if (ctx.getHistory().size() > existingCount) {
            List<Message> newMessages = ctx.getHistory().subList(existingCount, ctx.getHistory().size());
            List<AiMessage> entities = newMessages.stream()
                    .map(m -> AiMessage.builder()
                            .convId(convId)
                            .role(m.getRole())
                            .content(m.getContent())
                            .createdAt(toLocalDateTime(m.getTimestamp()))
                            .build())
                    .collect(Collectors.toList());
            if (!entities.isEmpty()) {
                messageMapper.insertBatch(entities);
            }
        }
    }

    /**
     * Redis miss 时回源重建 ConversationContext。
     * 返回 null 表示 MySQL 也没有。
     */
    public ConversationContext loadFromMySQL(String convId) {
        AiConversation conv = conversationMapper.selectByConvId(convId);
        if (conv == null) return null;

        List<AiMessage> messages = messageMapper.selectByConvId(convId);
        List<Message> history = messages.stream()
                .map(m -> Message.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .timestamp(m.getCreatedAt() != null ? m.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        return ConversationContext.builder()
                .conversationId(convId)
                .userId(String.valueOf(conv.getUserId()))
                .history(history)
                .createdAt(conv.getCreatedAt() != null ? conv.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .lastActiveAt(conv.getLastActiveAt() != null ? conv.getLastActiveAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .build();
    }

    /**
     * 对话列表查询。
     */
    public List<AiConversation> listByUserId(Long userId, int limit, int offset) {
        return conversationMapper.selectByUserId(userId, limit, offset);
    }

    /**
     * 软删。
     */
    public void softDelete(String convId, Long userId) {
        conversationMapper.softDelete(convId, userId);
    }

    /**
     * 重命名。
     */
    public void updateTitle(String convId, String title) {
        conversationMapper.updateTitle(convId, title);
    }

    /**
     * 将匿名对话归属到真实用户。
     */
    public int claimConversations(long realUserId) {
        return conversationMapper.claimConversations(0L, realUserId);
    }

    private String extractTitle(List<Message> history) {
        return history.stream()
                .filter(m -> "user".equals(m.getRole()))
                .map(Message::getContent)
                .findFirst()
                .map(s -> s.length() > 20 ? s.substring(0, 20) + "..." : s)
                .orElse("新对话");
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) return LocalDateTime.now();
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
