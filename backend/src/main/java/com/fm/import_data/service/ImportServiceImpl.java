package com.fm.import_data.service;

import com.fm.account.repository.entity.AccountDO;
import com.fm.account.repository.service.IAccountService;
import com.fm.common.Constants;
import com.fm.common.event.AssetCacheEvictEvent;
import com.fm.common.util.LogUtil;
import com.fm.import_data.controller.vo.ImportRespVO;
import com.fm.transaction.repository.entity.TransactionDO;
import com.fm.transaction.repository.service.ITransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Obsidian Markdown 数据导入服务实现
 *
 * 解析逻辑：委托给 MarkdownTableParser，结果按行入库，失败行记录到 failReasons
 */
@Slf4j
@Service
public class ImportServiceImpl implements ImportService {

    private final IAccountService iAccountService;
    private final ITransactionService iTransactionService;
    private final MarkdownTableParser markdownTableParser;
    private final ApplicationEventPublisher eventPublisher;

    public ImportServiceImpl(IAccountService iAccountService,
                             ITransactionService iTransactionService,
                             MarkdownTableParser markdownTableParser,
                             ApplicationEventPublisher eventPublisher) {
        this.iAccountService = iAccountService;
        this.iTransactionService = iTransactionService;
        this.markdownTableParser = markdownTableParser;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportRespVO importFromMarkdown(Long accountId, String markdownContent) {
        String traceId = LogUtil.getTraceId();

        AccountDO account = iAccountService.getById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("账户不存在，accountId=" + accountId);
        }
        if (markdownContent == null || markdownContent.isBlank()) {
            throw new IllegalArgumentException("Markdown内容为空");
        }

        List<ParsedRow> rows = markdownTableParser.parse(markdownContent);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("未解析到有效数据，请检查Markdown表格格式");
        }

        ImportRespVO resp = new ImportRespVO();
        resp.setTotalRows(rows.size());

        List<TransactionDO> toSave = new ArrayList<>();
        List<String> failReasons = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < rows.size(); i++) {
            ParsedRow row = rows.get(i);
            if (!row.isSuccess()) {
                failReasons.add(String.format("第%d行：%s", i + 1, row.getErrorReason()));
                continue;
            }
            TransactionDO tx = new TransactionDO();
            tx.setAccountId(accountId);
            tx.setAmount(row.getAmount());
            tx.setDate(today);
            tx.setSymbol(row.getSymbol());
            tx.setType(Constants.TransType.BUY);
            toSave.add(tx);
        }

        if (toSave.isEmpty()) {
            resp.setSuccessCount(0);
            resp.setFailCount(rows.size());
            resp.setFailReasons(failReasons);
            resp.setMessage("无有效数据可导入，失败 " + failReasons.size() + " 条");
            log.warn("[importFromMarkdown] 无有效数据 traceId={}, accountId={}, fails={}",
                    traceId, accountId, failReasons.size());
            return resp;
        }

        iTransactionService.saveBatch(toSave);
        eventPublisher.publishEvent(new AssetCacheEvictEvent("ImportService", AssetCacheEvictEvent.REASON_IMPORT));

        resp.setSuccessCount(toSave.size());
        resp.setFailCount(failReasons.size());
        resp.setFailReasons(failReasons);
        resp.setMessage(String.format("成功导入%d条，失败%d条", toSave.size(), failReasons.size()));

        log.info("[importFromMarkdown] 完成 traceId={}, accountId={}, success={}, fail={}",
                traceId, accountId, toSave.size(), failReasons.size());
        return resp;
    }
}
