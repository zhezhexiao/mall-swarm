package com.macro.mall.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "mall-ai.conversation")
public class ConversationProperties {
    private Duration ttl = Duration.ofMinutes(30);
    private int maxHistoryRounds = 10;
}
