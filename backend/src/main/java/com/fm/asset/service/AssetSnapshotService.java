package com.fm.asset.service;

import com.fm.asset.repository.entity.AssetSnapshotDO;
import com.fm.asset.repository.service.IAssetSnapshotService;
import com.fm.asset.service.bo.AssetSnapshotBO;
import com.fm.common.util.LogUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 资产快照业务服务
 *
 * 职责：
 * 1. 定时任务将每日资产总览写入快照表
 * 2. 业务 API 读取快照用于趋势图
 */
@Slf4j
@Service
public class AssetSnapshotService {

    private final IAssetSnapshotService iAssetSnapshotService;
    private final ObjectMapper objectMapper;

    public AssetSnapshotService(IAssetSnapshotService iAssetSnapshotService,
                                ObjectMapper objectMapper) {
        this.iAssetSnapshotService = iAssetSnapshotService;
        this.objectMapper = objectMapper;
    }

    /**
     * 将资产总览结果落库到快照表（upsert 语义，按日期去重）
     *
     * @param snapshotDate 快照日期
     * @param totalAsset   当日总资产
     * @param allocations  配置占比列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveSnapshot(LocalDate snapshotDate,
                             BigDecimal totalAsset,
                             List<AssetSnapshotBO.AllocationItem> allocations) {
        String traceId = LogUtil.getTraceId();
        log.info("[saveSnapshot] 开始写入 traceId={}, date={}, total={}", traceId, snapshotDate, totalAsset);

        try {
            String allocationJson = objectMapper.writeValueAsString(allocations == null ? new ArrayList<>() : allocations);

            AssetSnapshotDO existing = iAssetSnapshotService.lambdaQuery()
                    .eq(AssetSnapshotDO::getSnapshotDate, snapshotDate)
                    .one();
            AssetSnapshotDO entity;
            if (existing != null) {
                entity = new AssetSnapshotDO();
                entity.setId(existing.getId());
                entity.setSnapshotDate(snapshotDate);
                entity.setTotalAsset(totalAsset == null ? BigDecimal.ZERO : totalAsset);
                entity.setAllocationJson(allocationJson);
                iAssetSnapshotService.updateById(entity);
                log.info("[saveSnapshot] 更新已存在快照 traceId={}, id={}", traceId, existing.getId());
            } else {
                entity = new AssetSnapshotDO();
                entity.setSnapshotDate(snapshotDate);
                entity.setTotalAsset(totalAsset == null ? BigDecimal.ZERO : totalAsset);
                entity.setAllocationJson(allocationJson);
                iAssetSnapshotService.save(entity);
                log.info("[saveSnapshot] 新建快照 traceId={}, id={}", traceId, entity.getId());
            }
        } catch (Exception e) {
            log.error("[saveSnapshot] 写入失败 traceId={}, date={}, error={}", traceId, snapshotDate, e.getMessage(), e);
            throw new IllegalStateException("资产快照写入失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询最近 N 天快照
     */
    public List<AssetSnapshotBO> getRecentSnapshots(int days) {
        if (days <= 0) {
            return new ArrayList<>();
        }
        LocalDate startDate = LocalDate.now().minusDays(days - 1L);

        List<AssetSnapshotDO> entities = iAssetSnapshotService.lambdaQuery()
                .ge(AssetSnapshotDO::getSnapshotDate, startDate)
                .orderByAsc(AssetSnapshotDO::getSnapshotDate)
                .list();

        List<AssetSnapshotBO> result = new ArrayList<>(entities.size());
        for (AssetSnapshotDO e : entities) {
            result.add(toBO(e));
        }
        return result;
    }

    private AssetSnapshotBO toBO(AssetSnapshotDO entity) {
        AssetSnapshotBO bo = new AssetSnapshotBO();
        bo.setId(entity.getId());
        bo.setSnapshotDate(entity.getSnapshotDate());
        bo.setTotalAsset(entity.getTotalAsset());
        if (entity.getAllocationJson() != null && !entity.getAllocationJson().isBlank()) {
            try {
                List<AssetSnapshotBO.AllocationItem> items = objectMapper.readValue(
                        entity.getAllocationJson(),
                        new TypeReference<List<AssetSnapshotBO.AllocationItem>>() {});
                bo.setAllocationList(items);
            } catch (Exception e) {
                log.warn("[toBO] allocationJson 解析失败 id={}, json={}", entity.getId(), entity.getAllocationJson(), e);
                bo.setAllocationList(new ArrayList<>());
            }
        } else {
            bo.setAllocationList(new ArrayList<>());
        }
        return bo;
    }
}
