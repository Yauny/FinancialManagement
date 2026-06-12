package com.fm.common.event;

/**
 * 资产缓存失效事件
 *
 * 任何改变账户/交易数据的服务发布此事件，AssetService 监听并清除资产相关缓存。
 * 使用 Spring 事件机制解耦业务模块与缓存逻辑。
 */
public class AssetCacheEvictEvent {

    public static final String REASON_TRANSACTION_CHANGE = "transaction_change";
    public static final String REASON_ACCOUNT_CHANGE = "account_change";
    public static final String REASON_IMPORT = "import";

    private final String source;
    private final String reason;

    public AssetCacheEvictEvent(String source, String reason) {
        this.source = source;
        this.reason = reason;
    }

    public String getSource() {
        return source;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "AssetCacheEvictEvent{source='" + source + "', reason='" + reason + "'}";
    }
}
