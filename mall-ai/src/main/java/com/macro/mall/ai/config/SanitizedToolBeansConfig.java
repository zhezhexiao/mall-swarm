package com.macro.mall.ai.config;

import com.macro.mall.ai.tool.order.OrderTool;
import com.macro.mall.ai.tool.product.ProductTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 注册 sanitized ToolCallback 为 Spring Bean，使其进入
 * StaticToolCallbackResolver 的静态列表，确保 Tool 执行时走 sanitize 逻辑。
 *
 * DeepSeek 偶发尾部逗号 JSON → SanitizedToolCallbackWrapper.cleanJson() 清洗。
 */
@Slf4j
@Configuration
public class SanitizedToolBeansConfig {

    @Bean
    List<FunctionCallback> sanitizedToolCallbacks(ProductTool productTool, OrderTool orderTool) {
        MethodToolCallbackProvider raw = MethodToolCallbackProvider.builder()
                .toolObjects(productTool, orderTool)
                .build();

        FunctionCallback[] sanitized = SanitizedToolCallbackWrapper.wrap(raw);
        List<FunctionCallback> list = new ArrayList<>(Arrays.asList(sanitized));
        log.info("Registered {} sanitized tool callbacks", list.size());
        return list;
    }
}
