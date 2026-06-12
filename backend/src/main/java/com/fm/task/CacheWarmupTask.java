package com.fm.task;

import com.fm.asset.controller.vo.AssetSummaryRespVO;
import com.fm.asset.controller.vo.HoldingsRespVO;
import com.fm.asset.service.AssetService;
import com.fm.common.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 缓存预热定时任务
 *
 * 每日 02:30 执行：
 * 1. 调用 AssetService.getSummary() 触发 @Cacheable
 * 2. 调用 AssetService.getHoldings() 触发 @Cacheable
 *
 * 避免每日业务高峰期首次访问时的缓存击穿
 */
@Slf4j
@Component
public class CacheWarmupTask {

    private final AssetService assetService;

    public CacheWarmupTask(AssetService assetService) {
        this.assetService = assetService;
    }

    @Scheduled(cron = "${task.cache-warmup-cron:0 30 2 * * ?}")
    public void warmupCaches() {
        String traceId = LogUtil.generateTraceId();
        long start = System.currentTimeMillis();

        log.info("[CacheWarmupTask] 开始执行 traceId={}", traceId);
        try {
            AssetSummaryRespVO summary = assetService.getSummary();
            HoldingsRespVO holdings = assetService.getHoldings();

            long cost = System.currentTimeMillis() - start;
            log.info("[CacheWarmupTask] 执行完成 traceId={}, cost={}ms, summary={}, holdings={}",
                    traceId, cost,
                    summary == null ? "null" : "ok",
                    holdings == null ? "null" : "ok");
        } catch (Exception e) {
            log.error("[CacheWarmupTask] 执行异常 traceId={}, error={}", traceId, e.getMessage(), e);
        } finally {
            LogUtil.clear();
        }
    }
}
