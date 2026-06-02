package com.macro.mall.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingHints {
    private String lastRoute;
    private boolean faqHit;
    private boolean cacheHit;
}
