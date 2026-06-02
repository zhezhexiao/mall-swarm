package com.macro.mall.ai.tool.product;

import com.macro.mall.ai.integration.mallsearch.MallSearchClient;
import com.macro.mall.ai.model.ProductItem;
import com.macro.mall.ai.model.ProductSearchResult;
import com.macro.mall.ai.model.ProductSort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 商品搜索 Tool。
 *
 * Phase1: 单 Tool — searchProducts。
 * 使用 Spring AI @Tool 注解自动注册 schema。
 *
 * 搜索结果同时缓存到 AtomicReference，供 ChatApplicationService
 * 在 SSE 流结束后以结构化 JSON 事件 [PRODUCTS] 直传前端。
 */
@Slf4j
@Component
public class ProductTool {

    private static final AtomicReference<List<ProductItem>> LAST_PRODUCTS = new AtomicReference<>();

    private final MallSearchClient mallSearchClient;

    public ProductTool(MallSearchClient mallSearchClient) {
        this.mallSearchClient = mallSearchClient;
    }

    /** 获取最近一次搜索的结构化商品列表，调用后自动清除。 */
    public static List<ProductItem> drainLastProducts() {
        List<ProductItem> items = LAST_PRODUCTS.getAndSet(null);
        return (items != null && !items.isEmpty()) ? items : null;
    }

    @Tool(description = """
            搜索 mall 电商平台商品。
            用于商品搜索、商品推荐、商品比较等场景。
            支持关键词、品牌、价格上限和排序。
            """)
    public String searchProducts(
            @ToolParam(description = "商品关键词，如蓝牙耳机、T恤、手机") String keyword,
            @ToolParam(description = "品牌名称，可为空") String brandName,
            @ToolParam(description = "价格上限（元），可为空。只返回价格不超过此值的商品") BigDecimal maxPrice,
            @ToolParam(description = "排序方式") ProductSort sortBy,
            @ToolParam(description = "返回数量，默认5，最大10") Integer limit) {

        int size = limit != null ? Math.min(limit, 10) : 5;

        log.info("ProductTool.search: keyword='{}', brand='{}', maxPrice={}, sort={}, limit={}",
                keyword, brandName, maxPrice, sortBy, size);

        List<ProductItem> items = mallSearchClient.search(
                keyword, brandName, maxPrice, sortBy, size);

        log.info("ProductTool.search: returned {} items", items.size());

        // 缓存结构化数据，供 SSE 事件直传前端
        LAST_PRODUCTS.set(items);

        ProductSearchResult result = new ProductSearchResult(items.size(), items);
        return result.toFormattedText();
    }
}
