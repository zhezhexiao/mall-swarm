package com.macro.mall.ai.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class ToolResult {
    private String toolName;
    private boolean success;
    private Map<String, Object> data;
    private String error;
}
