package com.macro.mall.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversation {
    private Long id;
    private String convId;
    private Long userId;
    private String claimStatus;
    private String title;
    private Integer messageCount;
    private Boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
}
