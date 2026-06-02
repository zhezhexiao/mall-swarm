package com.macro.mall.ai.integration.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.macro.mall.ai.config.ConversationProperties;
import com.macro.mall.ai.conversation.ConversationContext;
import com.macro.mall.ai.conversation.ConversationStore;
import com.macro.mall.ai.conversation.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class RedisConversationStore implements ConversationStore {

    private final StringRedisTemplate redis;
    private final ConversationProperties props;
    private final ObjectMapper objectMapper;

    public RedisConversationStore(StringRedisTemplate redis, ConversationProperties props) {
        this.redis = redis;
        this.props = props;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public ConversationContext load(String conversationId) {
        String key = RedisKeys.conversation(conversationId);
        String json = redis.opsForValue().get(key);
        if (json == null) return null;

        // refresh TTL on read
        redis.expire(key, props.getTtl());

        try {
            return objectMapper.readValue(json, ConversationContext.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize conversation {}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    @Override
    public void save(ConversationContext ctx) {
        String key = RedisKeys.conversation(ctx.getConversationId());
        try {
            String json = objectMapper.writeValueAsString(ctx);
            redis.opsForValue().set(key, json, props.getTtl());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize conversation {}: {}", ctx.getConversationId(), e.getMessage());
        }
    }

    @Override
    public void expire(String conversationId) {
        redis.delete(RedisKeys.conversation(conversationId));
    }
}
