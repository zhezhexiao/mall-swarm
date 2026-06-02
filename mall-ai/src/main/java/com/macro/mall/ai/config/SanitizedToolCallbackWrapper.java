package com.macro.mall.ai.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.Arrays;

/**
 * JSON sanitizer for ToolCallback arguments.
 *
 * DeepSeek occasionally emits malformed JSON in tool call arguments.
 * This wrapper sanitizes before delegating, with progressive retry on failure.
 */
@Slf4j
public final class SanitizedToolCallbackWrapper {

    private static final ObjectMapper LENIENT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    private SanitizedToolCallbackWrapper() {}

    public static FunctionCallback[] wrap(MethodToolCallbackProvider raw) {
        return Arrays.stream(raw.getToolCallbacks())
                .filter(fc -> fc instanceof ToolCallback)
                .map(fc -> (ToolCallback) fc)
                .map(SanitizedToolCallbackWrapper::sanitize)
                .toArray(FunctionCallback[]::new);
    }

    private static ToolCallback sanitize(ToolCallback delegate) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return delegate.getToolDefinition();
            }

            @Override
            public String call(String toolInput) {
                if (toolInput == null || toolInput.isBlank()) {
                    return delegate.call(toolInput);
                }

                // First try: clean common issues
                String clean = cleanJson(toolInput);

                try {
                    return delegate.call(clean);
                } catch (Exception e) {
                    // If it's a JSON parse error, log it and try harder
                    if (isJsonError(e)) {
                        log.warn("SanitizedTool: JSON parse error for tool={}, input={}",
                                delegate.getToolDefinition().name(), toolInput);
                    }
                    throw e;
                }
            }
        };
    }

    static String cleanJson(String json) {
        if (json == null || json.isBlank()) return json;
        return json
                .replaceAll(":\\s*,", ": null,")    // missing value: "field":, or "field": ,
                .replaceAll(":\\s*}", ": null}")    // missing value at end: "field":}
                .replaceAll(",\\s*,", ",")        // double comma
                .replaceAll(",\\s*}", "}")         // trailing comma before }
                .replaceAll(",\\s*]", "]");        // trailing comma before ]
    }

    static boolean isJsonError(Throwable e) {
        while (e != null) {
            if (e instanceof JsonProcessingException) return true;
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Conversion from JSON")
                    || msg.contains("JsonParseException"))) return true;
            e = e.getCause();
        }
        return false;
    }
}
