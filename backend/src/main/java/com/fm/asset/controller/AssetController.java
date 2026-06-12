package com.fm.asset.controller;

import com.fm.asset.controller.vo.AssetSummaryRespVO;
import com.fm.asset.controller.vo.HoldingsRespVO;
import com.fm.asset.service.AssetService;
import com.fm.common.ApiResponse;
import com.fm.common.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
