package com.fm.import_data.service;

import com.fm.account.repository.entity.AccountDO;
import com.fm.account.repository.service.IAccountService;
import com.fm.common.Constants;
import com.fm.import_data.controller.vo.ImportRespVO;
import com.fm.transaction.repository.entity.TransactionDO;
import com.fm.transaction.repository.service.ITransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obsidian Markdown 数据导入服务实现
 *
 * 支持格式：
 * | 品种 | 代码 | 金额 | 占比 | 费率 | 评价 |
 * | 医疗基金（小荷包） | - | 11,543.93 元 | 41% | 0 | ✅ 存钱账户，继续攒 |
 */
@Slf4j
@Service
public class ImportServiceImpl implements ImportService {

    private static final Pattern ROW_PATTERN = Pattern.compile(
            "\\|\\s*([^|]+?)\\s*\\|\\s*([^|]*?)\\s*\\|\\s*([\\d,]+\\.?\\d*)\\s*元?\\s*\\|",
            Pattern.MULTILINE
    );

    private final IAccountService iAccountService;
    private final ITransactionService iTransactionService;

    public ImportServiceImpl(IAccountService iAccountService,
                             ITransactionService iTransactionService) {
        this.iAccountService = iAccountService;
        this.iTransactionService = iTransactionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportRespVO importFromMarkdown(Long accountId, String markdownContent) {
        AccountDO account = iAccountService.getById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("账户不存在，accountId=" + accountId);
        }

        if (markdownContent == null || markdownContent.isBlank()) {
            throw new IllegalArgumentException("Markdown内容为空");
        }

        ImportRespVO resp = new ImportRespVO();
        List<TransactionDO> toSave = new ArrayList<>();
        int failCount = 0;

        Matcher matcher = ROW_PATTERN.matcher(markdownContent);
        int totalRows = 0;

        while (matcher.find()) {
            totalRows++;
            String symbol = matcher.group(1).trim();
            String amountStr = matcher.group(3).trim().replace(",", "");

            if (symbol.isEmpty() || amountStr.isEmpty()) {
                failCount++;
                continue;
            }

            try {
                BigDecimal amount = new BigDecimal(amountStr);
                TransactionDO tx = new TransactionDO();
                tx.setAccountId(accountId);
                tx.setAmount(amount);
                tx.setDate(LocalDate.now());
                tx.setSymbol(symbol);
                tx.setType(Constants.TransType.BUY);
                toSave.add(tx);
            } catch (NumberFormatException e) {
                log.warn("[importFromMarkdown] 金额解析失败 symbol={}, amount={}", symbol, amountStr);
                failCount++;
            }
        }

        if (toSave.isEmpty()) {
            throw new IllegalArgumentException("未解析到有效数据，请检查Markdown表格格式");
        }

        iTransactionService.saveBatch(toSave);

        resp.setTotalRows(totalRows);
        resp.setSuccessCount(toSave.size());
        resp.setFailCount(failCount);
        resp.setMessage(String.format("成功导入%d条，失败%d条", toSave.size(), failCount));
        return resp;
    }
}
