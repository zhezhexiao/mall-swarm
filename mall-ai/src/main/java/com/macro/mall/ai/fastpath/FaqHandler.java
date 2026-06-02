package com.macro.mall.ai.fastpath;

import org.springframework.stereotype.Component;

@Component
public class FaqHandler {
    // Phase1: keyword matching only
    // Phase2: upgrade to Knowledge RAG
    public boolean matches(String message) { return false; }
    public String handle(String message) { return null; }
}
