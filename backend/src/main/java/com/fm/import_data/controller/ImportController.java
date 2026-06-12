package com.fm.import_data.controller;

import com.fm.common.ApiResponse;
import com.fm.common.util.LogUtil;
import com.fm.import_data.controller.vo.ImportReqVO;
import com.fm.import_data.controller.vo.ImportRespVO;
import com.fm.import_data.service.ImportService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 数据导入 REST 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/markdown")
    public ResponseEntity<ApiResponse<ImportRespVO>> importMarkdown(@RequestBody @Valid ImportReqVO reqVO) {
        String traceId = LogUtil.generateTraceId();
        long start = System.currentTimeMillis();
        LogUtil.logRequest(log, "importMarkdown", traceId, reqVO);

        try {
            ImportRespVO result = importService.importFromMarkdown(
                    reqVO.getAccountId(),
                    reqVO.getMarkdownContent()
            );
            long cost = System.currentTimeMillis() - start;
            LogUtil.logComplete(log, "importMarkdown", traceId, cost, result.getSuccessCount());
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.success(traceId, result));
        } catch (IllegalArgumentException e) {
            LogUtil.logFail(log, "importMarkdown", traceId, e.getMessage());
            LogUtil.clear();
            return ResponseEntity.ok(ApiResponse.fail(traceId, e.getMessage()));
        }
    }
}
