-- 认证密钥表
CREATE TABLE IF NOT EXISTS auth_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    auth_key VARCHAR(64) NOT NULL UNIQUE COMMENT '认证密钥',
    max_count INT NOT NULL DEFAULT 100 COMMENT '最大使用次数',
    used_count INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-有效，0-无效',
    description VARCHAR(255) COMMENT '密钥描述',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expire_time TIMESTAMP NULL COMMENT '过期时间',
    last_used_time TIMESTAMP NULL COMMENT '最后使用时间',
    last_used_ip VARCHAR(45) COMMENT '最后使用IP'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证密钥表';

-- 邮件操作日志表
CREATE TABLE IF NOT EXISTS email_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    auth_key VARCHAR(64) NOT NULL COMMENT '使用的认证密钥',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型：GENERATE_EMAIL, GET_VERIFICATION',
    email_address VARCHAR(255) COMMENT '邮箱地址',
    result VARCHAR(20) NOT NULL COMMENT '操作结果：SUCCESS, FAILED',
    error_message TEXT COMMENT '错误信息',
    request_ip VARCHAR(45) COMMENT '请求IP',
    user_agent VARCHAR(500) COMMENT '用户代理',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    response_time BIGINT COMMENT '响应时间(毫秒)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件操作日志表';

-- 临时邮箱管理表
CREATE TABLE IF NOT EXISTS temp_emails (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    email_address VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱地址',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '邮箱状态：ACTIVE-正常, BANNED-已封禁',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    remarks VARCHAR(500) COMMENT '备注信息',
    auth_key VARCHAR(64) NOT NULL COMMENT '授权密钥（用于数据隔离）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='临时邮箱管理表';

-- 创建索引（MySQL 5.7兼容语法）
-- 注意：auth_key字段已经是UNIQUE，自动有索引，无需重复创建
-- CREATE INDEX idx_auth_keys_key ON auth_keys(auth_key);
CREATE INDEX idx_auth_keys_status ON auth_keys(status);
CREATE INDEX idx_email_logs_auth_key ON email_logs(auth_key);
CREATE INDEX idx_email_logs_create_time ON email_logs(create_time);
CREATE INDEX idx_email_logs_operation_type ON email_logs(operation_type);

-- 临时邮箱表索引
CREATE INDEX idx_temp_emails_status ON temp_emails(status);
CREATE INDEX idx_temp_emails_expire_time ON temp_emails(expire_time);
CREATE INDEX idx_temp_emails_create_time ON temp_emails(create_time);
CREATE INDEX idx_temp_emails_auth_key ON temp_emails(auth_key);
CREATE INDEX idx_temp_emails_auth_key_status ON temp_emails(auth_key, status);
