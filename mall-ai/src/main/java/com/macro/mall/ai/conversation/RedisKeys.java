package com.macro.mall.ai.conversation;

public final class RedisKeys {

    private static final String PREFIX = "mall:ai:conv:";

    private RedisKeys() {}

    public static String conversation(String conversationId) {
        return PREFIX + conversationId;
    }
}
