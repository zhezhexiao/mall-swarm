package com.macro.mall.ai.integration.mallorder;

import com.macro.mall.ai.model.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * mall-admin 订单查询客户端。
 *
 * 调用 mall-admin (8080) 的 /order/list 接口。
 */
@Slf4j
@Component
public class MallOrderClient {

    @Value("${mall-ai.integration.mall-admin.base-url:http://localhost:8080}")
    private String adminUrl;

    private final RestClient restClient;

    public MallOrderClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 查询用户订单。
     *
     * @param userId   用户 ID（必须）
     * @param orderSn  订单号，可为 null
     * @param status   订单状态，可为 null
     * @param pageNum  页码
     * @param pageSize 每页大小
     */
    @SuppressWarnings("unchecked")
    public List<OrderItem> queryOrders(Long userId, String orderSn, Integer status, int pageNum, int pageSize) {
        // 验证用户身份
        if (userId == null) {
            log.warn("MallOrderClient: userId is null,拒绝查询");
            return Collections.emptyList();
        }

        StringBuilder url = new StringBuilder(adminUrl + "/order/list");
        url.append("?pageNum=").append(pageNum);
        url.append("&pageSize=").append(pageSize);
        url.append("&memberId=").append(userId);  // 关键：限制用户范围

        // 过滤 orderSn（防注入，只允许数字）
        if (orderSn != null && !orderSn.isBlank() && orderSn.matches("\\d+")) {
            url.append("&orderSn=").append(orderSn);
        }
        if (status != null) {
            url.append("&status=").append(status);
        }

        log.debug("MallOrderClient: GET {}", url);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(url.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !Integer.valueOf(200).equals(response.get("code"))) {
                log.warn("MallOrderClient: non-200 response: {}", response);
                return Collections.emptyList();
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");

            return list.stream()
                    .map(MallOrderClient::toOrderItem)
                    .toList();
        } catch (Exception e) {
            log.error("MallOrderClient call failed", e);
            return Collections.emptyList();
        }
    }

    private static final String[] STATUS_TEXTS = {
            "待付款", "待发货", "已发货", "已完成", "已关闭", "无效订单"
    };

    private static OrderItem toOrderItem(Map<String, Object> raw) {
        int st = raw.get("status") != null ? ((Number) raw.get("status")).intValue() : 0;
        String statusText = st >= 0 && st < STATUS_TEXTS.length ? STATUS_TEXTS[st] : "未知";
        return new OrderItem(
                ((Number) raw.get("id")).longValue(),
                (String) raw.get("orderSn"),
                raw.get("createTime") != null ? LocalDateTime.parse(raw.get("createTime").toString().replace("T", " ").substring(0, 19).replace(" ", "T")) : null,
                raw.get("totalAmount") != null ? new BigDecimal(raw.get("totalAmount").toString()) : BigDecimal.ZERO,
                raw.get("payAmount") != null ? new BigDecimal(raw.get("payAmount").toString()) : BigDecimal.ZERO,
                st,
                statusText,
                (String) raw.get("receiverName")
        );
    }
}
