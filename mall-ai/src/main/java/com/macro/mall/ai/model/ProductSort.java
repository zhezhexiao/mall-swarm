package com.macro.mall.ai.model;

/**
 * 商品排序方式（enum 化，稳定 schema）。
 */
public enum ProductSort {
    RELEVANCE,   // 相关度
    SALES,       // 销量
    PRICE_ASC,   // 价格从低到高
    PRICE_DESC,  // 价格从高到低
    NEWEST       // 新品
}
