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
public class AiMessage {
    private Long id;
    private String convId;
    private String role;
    private String content;
    private String productsJson;
    private LocalDateTime createdAt;
}
