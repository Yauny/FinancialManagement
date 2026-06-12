package com.fm.transaction.controller;

import com.fm.common.ApiResponse;
import com.fm.common.util.LogUtil;
import com.fm.transaction.controller.vo.TransactionReqVO;
import com.fm.transaction.controller.vo.TransactionRespVO;
import com.fm.transaction.controller.vo.TransactionSaveVO;
import com.fm.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<TransactionRespVO>> getById(@RequestBody Map<String, Long> body) {
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
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody Map<String, Long> body) {
        String traceId = LogUtil.generateTraceId();
        Long id = body == null ? null : body.get("id");
        LogUtil.logRequest(log, "delete", traceId, id);

        if (id == null) {
            LogUtil.logFail(log, "delete", traceId, "id不能为空");
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, "id不能为空"));
        }

        transactionService.delete(id);
        LogUtil.logComplete(log, "delete", traceId, 0, 1);
        LogUtil.clear();

        return ResponseEntity.ok(ApiResponse.success(traceId));
    }
}
