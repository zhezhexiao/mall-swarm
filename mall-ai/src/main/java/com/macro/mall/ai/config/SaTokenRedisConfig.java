package com.macro.mall.ai.config;

import cn.dev33.satoken.dao.SaTokenDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Sa-Token 持久层配置 — 注册自定义 SaTokenDao（连接 Redis db0）
 * 读取 spring.data.redis.host/port，本地开发自动用 localhost，容器环境用 redis
 */
@Configuration
public class SaTokenRedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    @Primary
    public SaTokenDao saTokenDao() {
        return new MallAiSaTokenDao(redisHost, redisPort);
    }
}
