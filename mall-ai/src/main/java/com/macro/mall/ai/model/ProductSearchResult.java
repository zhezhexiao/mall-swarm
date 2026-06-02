package com.macro.mall.ai.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品搜索结果 — 业务模型。
 */
public record ProductSearchResult(
        int total,
        List<ProductItem> items
) {
    public static ProductSearchResult empty() {
        return new ProductSearchResult(0, List.of());
    }

    /**
     * 格式化为 LLM 友好的文本，确保销量等关键信息不丢失。
     */
    public String toFormattedText() {
        if (items == null || items.isEmpty()) {
            return "未找到相关商品。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(total).append(" 件商品：\n");
        for (int i = 0; i < items.size(); i++) {
            ProductItem item = items.get(i);
            sb.append(i + 1).append(". ").append(item.name());
            sb.append(" — 💰 ¥").append(item.price());
            if (item.sales() > 0) {
                sb.append(" | 销量 ").append(item.sales());
            }
            if (item.brand() != null && !item.brand().isBlank()) {
                sb.append(" | 品牌：").append(item.brand());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
