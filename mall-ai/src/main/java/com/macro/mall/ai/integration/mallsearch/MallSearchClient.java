package com.macro.mall.ai.integration.mallsearch;

import com.macro.mall.ai.model.ProductItem;
import com.macro.mall.ai.model.ProductSort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * mall-search 集成客户端。
 *
 * 调用 mall-search (8081) 的 REST API，返回业务模型 ProductItem。
 */
@Slf4j
@Component
public class MallSearchClient {

    @Value("${mall-ai.integration.mall-search.base-url:http://localhost:8081}")
    private String searchUrl;

    private final RestClient restClient;

    public MallSearchClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 搜索商品并后处理过滤。
     */
    public List<ProductItem> search(String keyword, String brandName,
                                     BigDecimal maxPrice, ProductSort sortBy, int limit) {
        // 1. 请求更多条，供后处理过滤
        int fetchSize = Math.min(limit * 4, 20);

        String url = UriComponentsBuilder.fromHttpUrl(searchUrl + "/esProduct/search")
                .queryParam("keyword", keyword != null ? keyword : "")
                .queryParam("pageNum", 0)
                .queryParam("pageSize", fetchSize)
                .queryParam("sort", mapSort(sortBy))
                .build()
                .toUriString();

        log.debug("MallSearchClient: GET {}", url);

        // 2. 调用 mall-search
        Map<String, Object> response;
        try {
            response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("MallSearchClient call failed", e);
            return Collections.emptyList();
        }

        // 3. 解析 CommonResult → CommonPage → EsProduct
        List<ProductItem> items = extractItems(response);

        // 4. 后处理过滤：品牌、价格
        var stream = items.stream();
        if (brandName != null && !brandName.isBlank()) {
            stream = stream.filter(p ->
                    p.brand() != null && p.brand().contains(brandName));
        }
        if (maxPrice != null) {
            stream = stream.filter(p ->
                    p.price() != null && p.price().compareTo(maxPrice) <= 0);
        }

        return stream.limit(limit).toList();
    }

    @SuppressWarnings("unchecked")
    private List<ProductItem> extractItems(Map<String, Object> response) {
        try {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
            return list.stream()
                    .map(MallSearchClient::toProductItem)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to parse mall-search response", e);
            return Collections.emptyList();
        }
    }

    private static ProductItem toProductItem(Map<String, Object> raw) {
        return new ProductItem(
                ((Number) raw.get("id")).longValue(),
                (String) raw.get("name"),
                raw.get("price") != null ? new BigDecimal(raw.get("price").toString()) : BigDecimal.ZERO,
                raw.get("sale") != null ? ((Number) raw.get("sale")).intValue() : 0,
                (String) raw.get("brandName"),
                (String) raw.get("pic")
        );
    }

    private static int mapSort(ProductSort sort) {
        if (sort == null) return 0;
        return switch (sort) {
            case SALES -> 2;
            case PRICE_ASC -> 3;
            case PRICE_DESC -> 4;
            case NEWEST -> 1;
            default -> 0; // RELEVANCE
        };
    }
}
