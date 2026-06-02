package com.macro.mall.ai.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单摘要 — Tool 返回给 LLM 的订单信息。
 */
public record OrderItem(
        long id,
        String orderSn,
        LocalDateTime createTime,
        BigDecimal totalAmount,
        BigDecimal payAmount,
        int status,
        String statusText,
        String receiverName
) {}
