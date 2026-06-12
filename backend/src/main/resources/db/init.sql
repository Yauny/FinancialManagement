-- 初始化数据库
CREATE DATABASE IF NOT EXISTS fm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fm;

-- 账户表
CREATE TABLE IF NOT EXISTS account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '账户名称',
    type ENUM('platform','account','bank','card') NOT NULL COMMENT '账户类型',
    platform VARCHAR(50) DEFAULT '' COMMENT '平台来源',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_type (type),
    INDEX idx_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户表';

-- 交易表
CREATE TABLE IF NOT EXISTS `transaction` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '外键，关联账户',
    amount DECIMAL(12,2) NOT NULL COMMENT '金额',
    date DATE NOT NULL COMMENT '交易日期',
    symbol VARCHAR(100) NOT NULL COMMENT '品种名称/代码',
    type ENUM('buy','sell','dividend','transfer') NOT NULL COMMENT '交易类型',
    note TEXT COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_date (date),
    INDEX idx_symbol (symbol),
    INDEX idx_type (type),
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易表';
