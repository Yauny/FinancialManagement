package com.fm.account.controller;

import com.fm.account.controller.vo.AccountReqVO;
import com.fm.account.controller.vo.AccountRespVO;
import com.fm.account.controller.vo.AccountSaveVO;
import com.fm.account.service.AccountService;
import com.fm.common.ApiResponse;
import com.fm.common.util.LogUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 账户管理 REST 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<AccountRespVO>>> listAccounts(@RequestBody(required = false) AccountReqVO reqVO) {
        String traceId = LogUtil.generateTraceId();
        long start = System.currentTimeMillis();
        LogUtil.logRequest(log, "listAccounts", traceId, reqVO);

        List<AccountRespVO> result = accountService.listAccounts(reqVO);

        long cost = System.currentTimeMillis() - start;
        LogUtil.logComplete(log, "listAccounts", traceId, cost, result.size());
        LogUtil.clear();

        return ResponseEntity.ok(ApiResponse.success(traceId, result));
    }

    @PostMapping("/get")
    public ResponseEntity<ApiResponse<AccountRespVO>> getById(@RequestBody java.util.Map<String, Long> body) {
        String traceId = LogUtil.generateTraceId();
        Long id = body == null ? null : body.get("id");
        LogUtil.logRequest(log, "getById", traceId, id);

        if (id == null) {
            LogUtil.logFail(log, "getById", traceId, "id不能为空");
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, "id不能为空"));
        }

        AccountRespVO result = accountService.getById(id);
        LogUtil.logComplete(log, "getById", traceId, 0, result == null ? 0 : 1);
        LogUtil.clear();

        return ResponseEntity.ok(ApiResponse.success(traceId, result));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Void>> save(@RequestBody @Valid AccountSaveVO saveVO) {
        String traceId = LogUtil.generateTraceId();
        LogUtil.logRequest(log, "save", traceId, saveVO);

        try {
            accountService.save(saveVO);
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
    public ResponseEntity<ApiResponse<Void>> update(@RequestBody @Valid AccountSaveVO saveVO) {
        String traceId = LogUtil.generateTraceId();
        LogUtil.logRequest(log, "update", traceId, saveVO);

        if (saveVO.getId() == null) {
            LogUtil.logFail(log, "update", traceId, "id不能为空");
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, "id不能为空"));
        }

        try {
            accountService.update(saveVO.getId(), saveVO);
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
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody java.util.Map<String, Long> body) {
        String traceId = LogUtil.generateTraceId();
        Long id = body == null ? null : body.get("id");
        LogUtil.logRequest(log, "delete", traceId, id);

        if (id == null) {
            LogUtil.logFail(log, "delete", traceId, "id不能为空");
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, "id不能为空"));
        }

        accountService.delete(id);
        LogUtil.logComplete(log, "delete", traceId, 0, 1);
        LogUtil.clear();

        return ResponseEntity.ok(ApiResponse.success(traceId));
    }
}
