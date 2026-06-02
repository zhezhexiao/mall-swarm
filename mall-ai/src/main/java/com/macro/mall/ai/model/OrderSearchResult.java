package com.macro.mall.ai.model;

import java.util.List;

/**
 * 订单搜索结果 — Tool 返回值。
 */
public record OrderSearchResult(
        int total,
        List<OrderItem> items
) {}
