package com.fm.asset.controller;

import com.fm.asset.controller.vo.AssetSnapshotRespVO;
import com.fm.asset.controller.vo.AssetSummaryRespVO;
import com.fm.asset.controller.vo.HoldingsRespVO;
import com.fm.asset.service.AssetService;
import com.fm.common.ApiResponse;
import com.fm.common.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 资产总览 REST 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping("/summary")
    public ResponseEntity<ApiResponse<AssetSummaryRespVO>> getSummary() {
        String traceId = LogUtil.generateTraceId();
        long start = System.currentTimeMillis();
        LogUtil.logRequest(log, "getSummary", traceId, null);

        AssetSummaryRespVO result = assetService.getSummary();

        long cost = System.currentTimeMillis() - start;
        LogUtil.logComplete(log, "getSummary", traceId, cost, result != null ? 1 : 0);
        LogUtil.clear();

        return ResponseEntity.ok(ApiResponse.success(traceId, result));
    }

    @PostMapping("/holdings")
    public ResponseEntity<ApiResponse<HoldingsRespVO>> getHoldings() {
        String traceId = LogUtil.generateTraceId();
        long start = System.currentTimeMillis();
        LogUtil.logRequest(log, "getHoldings", traceId, null);

        HoldingsRespVO result = assetService.getHoldings();

        long cost = System.currentTimeMillis() - start;
        LogUtil.logComplete(log, "getHoldings", traceId, cost, result != null ? 1 : 0);
        LogUtil.clear();

        return ResponseEntity.ok(ApiResponse.success(traceId, result));
    }

    @PostMapping("/snapshots")
    public ResponseEntity<ApiResponse<List<AssetSnapshotRespVO>>> getSnapshots(
            @RequestBody(required = false) Map<String, Integer> body) {
        String traceId = LogUtil.generateTraceId();
        long start = System.currentTimeMillis();
        Integer days = body == null ? null : body.get("days");
        LogUtil.logRequest(log, "getSnapshots", traceId, days);

        int effectiveDays = days == null ? 30 : days;
        if (effectiveDays <= 0) {
            LogUtil.logFail(log, "getSnapshots", traceId, "days 必须大于 0");
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, "days 必须大于 0"));
        }
        if (effectiveDays > 365) {
            effectiveDays = 365;
        }

        List<AssetSnapshotRespVO> result = assetService.getSnapshots(effectiveDays);

        long cost = System.currentTimeMillis() - start;
        LogUtil.logComplete(log, "getSnapshots", traceId, cost, result.size());
        LogUtil.clear();

        return ResponseEntity.ok(ApiResponse.success(traceId, result));
    }
}
