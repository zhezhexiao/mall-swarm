package com.macro.mall.ai.controller;

import com.macro.mall.ai.conversation.ConversationManager;
import com.macro.mall.ai.model.AiConversation;
import com.macro.mall.ai.util.StpMemberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationManager conversationManager;

    @GetMapping
    public List<AiConversation> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        try {
            Long userId = currentUserId();
            return conversationManager.listByUserId(userId, page, size);
        } catch (Exception e) {
            // 未登录 → 返回匿名对话
            return conversationManager.listByUserId(0L, page, size);
        }
    }

    @GetMapping("/{convId}")
    public AiConversation detail(@PathVariable String convId) {
        Long userId = currentUserId();
        // loadOrCreate 会从 Redis 或 MySQL 恢复
        conversationManager.loadOrCreate(convId, String.valueOf(userId));
        // 返回 conversation 元数据（不返回完整 history，前端通过 chat stream 获取）
        return conversationManager.listByUserId(userId, 0, 1000)
                .stream().filter(c -> c.getConvId().equals(convId))
                .findFirst().orElse(null);
    }

    @PostMapping("/claim")
    public Map<String, Object> claim() {
        Long userId = currentUserId();
        int count = conversationManager.claimConversations(userId);
        return Map.of("success", true, "claimed", count);
    }

    @DeleteMapping("/{convId}")
    public Map<String, Object> delete(@PathVariable String convId) {
        Long userId = currentUserId();
        conversationManager.delete(convId, userId);
        return Map.of("success", true);
    }

    @PatchMapping("/{convId}")
    public Map<String, Object> rename(@PathVariable String convId,
                                       @RequestBody Map<String, String> body) {
        String title = body.get("title");
        conversationManager.rename(convId, title);
        return Map.of("success", true);
    }

    private Long currentUserId() {
        try {
            Object loginId = StpMemberUtil.stpLogic.getLoginId();
            return Long.parseLong(String.valueOf(loginId));
        } catch (Exception e) {
            throw new RuntimeException("未登录");
        }
    }
}
