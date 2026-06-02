package com.macro.mall.ai.fastpath;

import org.springframework.stereotype.Component;

@Component
public class GreetingHandler {

    private static final String[] GREETINGS = {"你好", "hi", "hello", "嗨", "在吗"};

    public boolean matches(String message) {
        String lower = message.toLowerCase().trim();
        for (String g : GREETINGS) {
            if (lower.contains(g)) return true;
        }
        return false;
    }

    public String handle() {
        return "你好！我是 mall AI 助手，可以帮您推荐商品、查询订单。有什么需要吗？";
    }
}
