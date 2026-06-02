package com.macro.mall.ai.tool.order;

import com.macro.mall.ai.integration.mallorder.MallOrderClient;
import com.macro.mall.ai.model.OrderItem;
import com.macro.mall.ai.model.OrderSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单查询 Tool。
 *
 * userId 由 AI 从系统 prompt 获取并作为参数传入，
 * 不再依赖 Sa-Token ThreadLocal（异步线程不可用）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTool {

    private final MallOrderClient mallOrderClient;

    @Tool(description = """
            查询当前用户的订单。
            用于订单查询、订单状态跟踪、历史订单检索等场景。
            支持按订单号、订单状态过滤。
            必须传入当前用户 ID（从系统 prompt 获取）。
            """)
    public OrderSearchResult queryOrders(
            @ToolParam(description = "当前登录用户 ID（必填，从系统 prompt 获取）") Long userId,
            @ToolParam(description = "订单号，可为空。精确匹配") String orderSn,
            @ToolParam(description = "订单状态: 0=待付款, 1=待发货, 2=已发货, 3=已完成, 4=已关闭, 5=无效订单。可为空表示不限") Integer status,
            @ToolParam(description = "返回数量，默认5，最大10") Integer limit) {

        if (userId == null) {
            log.warn("OrderTool.query: userId is null, user may not be logged in");
            return new OrderSearchResult(0, List.of());
        }

        log.info("OrderTool.query: userId={}, orderSn='{}', status={}, limit={}", userId, orderSn, status, limit);

        int size = limit != null ? Math.min(limit, 10) : 5;

        List<OrderItem> items = mallOrderClient.queryOrders(userId, orderSn, status, 1, size);

        log.info("OrderTool.query: returned {} items", items.size());

        return new OrderSearchResult(items.size(), items);
    }
}
