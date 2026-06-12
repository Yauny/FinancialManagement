package com.fm.task;

import com.fm.asset.controller.vo.AssetSummaryRespVO;
import com.fm.asset.service.AssetService;
import com.fm.asset.service.AssetSnapshotService;
import com.fm.asset.service.bo.AssetSnapshotBO;
import com.fm.common.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 资产快照定时任务
 *
 * 每日 02:00 执行：
 * 1. 调用 AssetService.getSummary() 获取当前总览
 * 2. 将数据写入 asset_snapshot 表（按日期 upsert）
 * 3. 失败时记录日志，不中断后续定时任务
 */
@Slf4j
@Component
public class AssetSnapshotTask {

    private final AssetService assetService;
    private final AssetSnapshotService assetSnapshotService;

    public AssetSnapshotTask(AssetService assetService,
                             AssetSnapshotService assetSnapshotService) {
        this.assetService = assetService;
        this.assetSnapshotService = assetSnapshotService;
    }

    @Scheduled(cron = "${task.asset-snapshot-cron:0 0 2 * * ?}")
    public void executeDailySnapshot() {
        String traceId = LogUtil.generateTraceId();
        long start = System.currentTimeMillis();

        log.info("[AssetSnapshotTask] 开始执行 traceId={}, date={}", traceId, LocalDate.now());
        try {
            AssetSummaryRespVO summary = assetService.getSummary();
            List<AssetSnapshotBO.AllocationItem> items = new ArrayList<>();
            if (summary != null && summary.getAllocationList() != null) {
                for (AssetSummaryRespVO.AllocationItem src : summary.getAllocationList()) {
                    AssetSnapshotBO.AllocationItem item = new AssetSnapshotBO.AllocationItem();
                    item.setType(src.getType());
                    item.setAmount(src.getAmount());
                    item.setRatio(src.getRatio());
                    items.add(item);
                }
            }
            assetSnapshotService.saveSnapshot(
                    LocalDate.now(),
                    summary == null ? null : summary.getTotalAsset(),
                    items
            );
            long cost = System.currentTimeMillis() - start;
            log.info("[AssetSnapshotTask] 执行完成 traceId={}, cost={}ms", traceId, cost);
        } catch (Exception e) {
            log.error("[AssetSnapshotTask] 执行异常 traceId={}, error={}", traceId, e.getMessage(), e);
        } finally {
            LogUtil.clear();
        }
    }
}
