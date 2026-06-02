package com.macro.mall.ai.config;

import cn.dev33.satoken.dao.SaTokenDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Sa-Token 持久层配置 — 注册自定义 SaTokenDao（连接 Redis db0）
 */
@Configuration
public class SaTokenRedisConfig {

    @Bean
    @Primary
    public SaTokenDao saTokenDao() {
        return new MallAiSaTokenDao();
    }
}
