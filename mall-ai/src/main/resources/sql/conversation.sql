-- mall-ai 对话持久化 DDL
-- 执行: docker exec mysql mysql -uroot -proot mall < mall-ai/src/main/resources/sql/conversation.sql

CREATE TABLE IF NOT EXISTS ai_conversation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conv_id         VARCHAR(36) NOT NULL COMMENT 'UUID',
    user_id         BIGINT NOT NULL COMMENT 'ums_member.id',
    title           VARCHAR(100) DEFAULT NULL COMMENT '首条用户消息截断',
    message_count   INT DEFAULT 0,
    deleted         TINYINT DEFAULT 0,
    deleted_at      DATETIME DEFAULT NULL,
    created_at      DATETIME NOT NULL,
    last_active_at  DATETIME NOT NULL,
    UNIQUE KEY uk_conv_id (conv_id),
    INDEX idx_user_active (user_id, last_active_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话';

CREATE TABLE IF NOT EXISTS ai_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conv_id         VARCHAR(36) NOT NULL,
    role            VARCHAR(10) NOT NULL COMMENT 'user/assistant/tool',
    content         TEXT NOT NULL,
    products_json   TEXT DEFAULT NULL COMMENT '结构化商品数据JSON',
    created_at      DATETIME NOT NULL,
    INDEX idx_conv (conv_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息';
