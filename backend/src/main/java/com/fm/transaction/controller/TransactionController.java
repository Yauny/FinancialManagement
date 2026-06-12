package com.fm.transaction.controller;

import com.fm.common.ApiResponse;
import com.fm.common.util.LogUtil;
import com.fm.transaction.controller.vo.TransactionReqVO;
import com.fm.transaction.controller.vo.TransactionRespVO;
import com.fm.transaction.controller.vo.TransactionSaveVO;
import com.fm.transaction.controller.vo.TransactionStatisticsRespVO;
import com.fm.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 交易记录 REST 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<TransactionRespVO>>> listTransactions(
            @RequestBody(required = false) TransactionReqVO reqVO) {
        String traceId = LogUtil.generateTraceId();
        long start = System.currentTimeMillis();
        LogUtil.logRequest(log, "listTransactions", traceId, reqVO);

        List<TransactionRespVO> result = transactionService.listTransactions(reqVO);

        long cost = System.currentTimeMillis() - start;
        LogUtil.logComplete(log, "listTransactions", traceId, cost, result.size());
        LogUtil.clear();

        return ResponseEntity.ok(ApiResponse.success(traceId, result));
    }

    @PostMapping("/get")
    public ResponseEntity<ApiResponse<TransactionRespVO>> getById(@RequestBody(required = false) Map<String, Long> body) {
        String traceId = LogUtil.generateTraceId();
        Long id = body == null ? null : body.get("id");
        LogUtil.logRequest(log, "getById", traceId, id);

        if (id == null) {
            LogUtil.logFail(log, "getById", traceId, "id不能为空");
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, "id不能为空"));
        }

        TransactionRespVO result = transactionService.getById(id);
        LogUtil.logComplete(log, "getById", traceId, 0, result == null ? 0 : 1);
        LogUtil.clear();

        return ResponseEntity.ok(ApiResponse.success(traceId, result));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Void>> save(@RequestBody @Valid TransactionSaveVO saveVO) {
        String traceId = LogUtil.generateTraceId();
        LogUtil.logRequest(log, "save", traceId, saveVO);

        try {
            transactionService.save(saveVO);
            LogUtil.logComplete(log, "save", traceId, 0, 1);
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.success(traceId));
        } catch (IllegalArgumentException e) {
            LogUtil.logFail(log, "save", traceId, e.getMessage());
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, e.getMessage()));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<Void>> update(@RequestBody @Valid TransactionSaveVO saveVO) {
        String traceId = LogUtil.generateTraceId();
        LogUtil.logRequest(log, "update", traceId, saveVO);

        if (saveVO.getId() == null) {
            LogUtil.logFail(log, "update", traceId, "id不能为空");
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, "id不能为空"));
        }

        try {
            transactionService.update(saveVO.getId(), saveVO);
            LogUtil.logComplete(log, "update", traceId, 0, 1);
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.success(traceId));
        } catch (IllegalArgumentException e) {
            LogUtil.logFail(log, "update", traceId, e.getMessage());
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, e.getMessage()));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody(required = false) Map<String, Long> body) {
        String traceId = LogUtil.generateTraceId();
        Long id = body == null ? null : body.get("id");
        LogUtil.logRequest(log, "delete", traceId, id);

        if (id == null) {
            LogUtil.logFail(log, "delete", traceId, "id不能为空");
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, "id不能为空"));
        }

        try {
            transactionService.delete(id);
            LogUtil.logComplete(log, "delete", traceId, 0, 1);
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.success(traceId));
        } catch (IllegalArgumentException e) {
            LogUtil.logFail(log, "delete", traceId, e.getMessage());
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, e.getMessage()));
        }
    }

    @PostMapping("/batchSave")
    public ResponseEntity<ApiResponse<Integer>> batchSave(@RequestBody(required = false) List<TransactionSaveVO> saveVOs) {
        String traceId = LogUtil.generateTraceId();
        LogUtil.logRequest(log, "batchSave", traceId,
                saveVOs == null ? 0 : saveVOs.size());

        if (saveVOs == null || saveVOs.isEmpty()) {
            LogUtil.logFail(log, "batchSave", traceId, "批量保存列表不能为空");
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, "批量保存列表不能为空"));
        }

        try {
            int count = transactionService.batchSave(saveVOs);
            LogUtil.logComplete(log, "batchSave", traceId, 0, count);
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.success(traceId, count));
        } catch (IllegalArgumentException e) {
            LogUtil.logFail(log, "batchSave", traceId, e.getMessage());
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, e.getMessage()));
        }
    }

    @PostMapping("/statistics")
    public ResponseEntity<ApiResponse<TransactionStatisticsRespVO>> statistics(
            @RequestBody(required = false) Map<String, String> body) {
        String traceId = LogUtil.generateTraceId();
        LocalDate start = body == null || body.get("startDate") == null
                ? null : LocalDate.parse(body.get("startDate"));
        LocalDate end = body == null || body.get("endDate") == null
                ? null : LocalDate.parse(body.get("endDate"));
        LogUtil.logRequest(log, "statistics", traceId,
                "startDate=" + start + ", endDate=" + end);

        try {
            TransactionStatisticsRespVO result = transactionService.getStatistics(start, end);
            LogUtil.logComplete(log, "statistics", traceId, 0, result == null ? 0 : 1);
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.success(traceId, result));
        } catch (IllegalArgumentException e) {
            LogUtil.logFail(log, "statistics", traceId, e.getMessage());
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, e.getMessage()));
        }
    }
}
