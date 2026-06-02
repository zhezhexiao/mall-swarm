package com.macro.mall.ai.tool;

import java.util.Map;

public interface Tool {
    String name();
    String description();
    Map<String, Object> execute(Map<String, Object> params);
}
