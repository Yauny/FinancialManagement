-- 初始化数据库（H2 MySQL 模式 + MySQL 8 双兼容）
-- H2 MySQL 模式支持 ENUM、ON UPDATE CURRENT_TIMESTAMP，不支持 USE/CREATE DATABASE

-- 账户表
CREATE TABLE IF NOT EXISTS account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '账户名称',
    type VARCHAR(20) NOT NULL COMMENT '账户类型：platform/account/bank/card',
    platform VARCHAR(50) DEFAULT '' COMMENT '平台来源',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_account_type ON account(type);
CREATE INDEX IF NOT EXISTS idx_account_platform ON account(platform);

-- 交易表
CREATE TABLE IF NOT EXISTS "transaction" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '外键，关联账户',
    amount DECIMAL(12,2) NOT NULL COMMENT '金额',
    date DATE NOT NULL COMMENT '交易日期',
    symbol VARCHAR(100) NOT NULL COMMENT '品种名称/代码',
    type VARCHAR(20) NOT NULL COMMENT '交易类型：buy/sell/dividend/transfer',
    note TEXT COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_tx_account_id ON "transaction"(account_id);
CREATE INDEX IF NOT EXISTS idx_tx_date ON "transaction"(date);
CREATE INDEX IF NOT EXISTS idx_tx_symbol ON "transaction"(symbol);
CREATE INDEX IF NOT EXISTS idx_tx_type ON "transaction"(type);

-- 资产快照表（定时任务每日写入，用于趋势图）
CREATE TABLE IF NOT EXISTS asset_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_date DATE NOT NULL UNIQUE COMMENT '快照日期',
    total_asset DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '当日总资产',
    allocation_json TEXT COMMENT '配置占比 JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_snapshot_date ON asset_snapshot(snapshot_date);
