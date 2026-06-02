package com.macro.mall.ai.tool;

import com.macro.mall.ai.conversation.ConversationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ToolRegistry {

    private final List<Tool> tools;

    public ToolRegistry(List<Tool> tools) {
        this.tools = tools;
    }

    public List<Tool> availableTools(ConversationContext ctx) {
        // Phase1: return all tools
        return tools;
    }
}
