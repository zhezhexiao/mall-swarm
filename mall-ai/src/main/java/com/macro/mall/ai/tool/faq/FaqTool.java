package com.macro.mall.ai.tool.faq;

import com.macro.mall.ai.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FaqTool implements Tool {

    @Override public String name() { return "searchFaq"; }

    @Override public String description() { return "搜索FAQ知识库"; }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        return Map.of("message", "FaqTool stub - Phase2");
    }
}
