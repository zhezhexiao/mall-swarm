package com.macro.mall.ai.model;

import java.math.BigDecimal;

/**
 * 精简商品条目 — 给 LLM 看的业务模型，不是 ES 镜像。
 */
public record ProductItem(
        long id,
        String name,
        BigDecimal price,
        int sales,
        String brand,
        String image
) {}
