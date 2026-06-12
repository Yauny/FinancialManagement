package com.fm.common.util;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 日志工具类，统一 traceId 管理
 */
public class LogUtil {

    private static final String TRACE_ID_KEY = "traceId";

    private LogUtil() {}

    public static String generateTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void clear() {
        MDC.clear();
    }

    public static void logRequest(Logger log, String method, String traceId, Object params) {
        log.info("[{}] 请求进入 traceId={}, params={}", method, traceId, params);
    }

    public static void logComplete(Logger log, String method, String traceId, long costMs, int resultCount) {
        log.info("[{}] 执行完成 traceId={}, cost={}ms, resultCount={}", method, traceId, costMs, resultCount);
    }

    public static void logFail(Logger log, String method, String traceId, String reason) {
        log.warn("[{}] 参数校验失败 traceId={}, reason={}", method, traceId, reason);
    }

    public static void logError(Logger log, String method, String traceId, Throwable e) {
        log.error("[{}] 执行异常 traceId={}, error={}", method, traceId, e.getMessage(), e);
    }
}
