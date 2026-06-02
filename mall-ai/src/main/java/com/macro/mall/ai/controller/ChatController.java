package com.macro.mall.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.macro.mall.ai.application.ChatApplicationService;
import com.macro.mall.ai.model.ChatRequest;
import com.macro.mall.ai.model.ChatResponse;
import com.macro.mall.ai.util.StpMemberUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatApplicationService chatApplicationService;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        resolveUserId(request);
        return chatApplicationService.chat(request);
    }

    /** SSE 流式对话。首个事件为 conversationId。 */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@Valid @RequestBody ChatRequest request) {
        resolveUserId(request);
        return chatApplicationService.chatStream(request)
                .map(text -> ServerSentEvent.<String>builder()
                        .data(text)
                        .build());
    }

    /**
     * 在请求线程（Sa-Token 上下文有效）中解析用户 ID。
     * SSE 端点排除了 Sa-Token 拦截器（Tomcat 异步分发问题），
     * 这里手动从 token 获取 userId 并注入 request。
     */
    private void resolveUserId(ChatRequest request) {
        if (request.getUserId() != null) return;
        try {
            org.springframework.web.context.request.RequestAttributes attrs =
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                String auth = sra.getRequest().getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    String tokenValue = auth.substring(7);
                    Object loginId = StpMemberUtil.stpLogic.getLoginIdByToken(tokenValue);
                    if (loginId != null) {
                        request.setUserId(String.valueOf(loginId));
                    }
                }
            }
        } catch (Exception e) {
            // token 无效或未登录 — userId 保持 null
        }
    }
}
