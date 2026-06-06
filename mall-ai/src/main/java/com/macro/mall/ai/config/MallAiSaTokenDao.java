package com.macro.mall.ai.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.session.SaSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token 持久层 — 连接 Redis db0（与 mall-portal 共享 Session）
 * 隔离于业务 Redis db1
 */
public class MallAiSaTokenDao implements SaTokenDao {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MallAiSaTokenDao(String host, int port) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        factory.afterPropertiesSet();
        this.redis = new StringRedisTemplate(factory);
    }

    @Override public String get(String key) {
        return redis.opsForValue().get(key);
    }
    @Override public void set(String key, String value, long timeout) {
        if (timeout <= 0) redis.opsForValue().set(key, value);
        else redis.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }
    @Override public void update(String key, String value) {
        if (Boolean.TRUE.equals(redis.hasKey(key))) redis.opsForValue().set(key, value);
    }
    @Override public void delete(String key) { redis.delete(key); }
    @Override public long getTimeout(String key) {
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        return ttl == null ? -2 : ttl;
    }
    @Override public void updateTimeout(String key, long timeout) {
        if (timeout > 0) redis.expire(key, timeout, TimeUnit.SECONDS);
    }

    // --- Object ---
    @Override public Object getObject(String key) {
        String json = get(key);
        if (json == null) return null;
        try { return objectMapper.readValue(json, SaSession.class); }
        catch (Exception e) { return null; }
    }
    @Override public <T> T getObject(String key, Class<T> clazz) {
        String json = get(key);
        if (json == null) return null;
        try { return objectMapper.readValue(json, clazz); }
        catch (Exception e) { return null; }
    }
    @Override public void setObject(String key, Object object, long timeout) {
        try { set(key, objectMapper.writeValueAsString(object), timeout); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }
    @Override public void updateObject(String key, Object object) {
        try { update(key, objectMapper.writeValueAsString(object)); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }
    @Override public void deleteObject(String key) { delete(key); }
    @Override public long getObjectTimeout(String key) { return getTimeout(key); }
    @Override public void updateObjectTimeout(String key, long timeout) { updateTimeout(key, timeout); }

    // --- Session ---
    @Override public SaSession getSession(String sessionId) {
        return (SaSession) getObject(sessionId);
    }
    @Override public void setSession(SaSession session, long timeout) {
        setObject(session.getId(), session, timeout);
    }
    @Override public void updateSession(SaSession session) {
        updateObject(session.getId(), session);
    }
    @Override public void deleteSession(String sessionId) { delete(sessionId); }
    @Override public long getSessionTimeout(String sessionId) { return getTimeout(sessionId); }
    @Override public void updateSessionTimeout(String sessionId, long timeout) { updateTimeout(sessionId, timeout); }

    // --- Search ---
    @Override public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        List<String> keys = new ArrayList<>();
        redis.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            var cursor = connection.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(prefix + "*" + keyword + "*").count(100).build());
            while (cursor.hasNext()) keys.add(new String(cursor.next()));
            cursor.close();
            return null;
        });
        if (sortType) Collections.sort(keys); else Collections.sort(keys, Collections.reverseOrder());
        int end = Math.min(start + size, keys.size());
        return start >= keys.size() ? Collections.emptyList() : keys.subList(start, end);
    }
}
