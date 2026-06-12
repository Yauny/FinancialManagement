package com.fm.common;

/**
 * 项目级常量
 */
public class Constants {

    private Constants() {}

    /**
     * 账户类型
     */
    public static final class AccountType {
        public static final String PLATFORM = "platform";
        public static final String ACCOUNT = "account";
        public static final String BANK = "bank";
        public static final String CARD = "card";
    }

    /**
     * 交易类型
     */
    public static final class TransType {
        public static final String BUY = "buy";
        public static final String SELL = "sell";
        public static final String DIVIDEND = "dividend";
        public static final String TRANSFER = "transfer";
    }

    /**
     * 定时任务 cron 表达式
     */
    public static final class TaskCron {
        /** 资产快照：每日凌晨 2:00 */
        public static final String ASSET_SNAPSHOT_DAILY = "0 0 2 * * ?";
        /** 缓存预热：每日凌晨 02:30 */
        public static final String CACHE_WARMUP_DAILY = "0 30 2 * * ?";
    }
}
