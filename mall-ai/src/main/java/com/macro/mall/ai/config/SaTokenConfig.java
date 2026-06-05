package com.macro.mall.ai.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.util.SaResult;
import com.macro.mall.ai.util.StpMemberUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 鉴权配置 — mall-ai 服务自身的第二层防御
 * 即使绕过 Gateway 直连 8002 端口，也必须登录才能访问 /ai/**
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpMemberUtil.checkLogin()))
                .addPathPatterns("/ai/**")
                // SSE 流式端点：Tomcat 异步分发时 Sa-Token ThreadLocal 已失效，
                // 会抛 SaTokenContextException 导致连接无法正常关闭。
                // 该端点的鉴权由 Gateway + 初始请求时的拦截器保障。
                .excludePathPatterns("/ai/chat/stream", "/ai/conversations");
    }
}
