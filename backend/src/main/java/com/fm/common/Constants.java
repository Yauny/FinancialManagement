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
}
