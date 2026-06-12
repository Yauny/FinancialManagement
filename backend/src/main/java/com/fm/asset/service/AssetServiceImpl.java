package com.fm.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.account.repository.entity.AccountDO;
import com.fm.account.repository.service.IAccountService;
import com.fm.asset.controller.vo.AssetSummaryRespVO;
import com.fm.asset.controller.vo.HoldingsRespVO;
import com.fm.common.Constants;
import com.fm.transaction.repository.entity.TransactionDO;
import com.fm.transaction.repository.service.ITransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产总览业务服务实现
 */
@Slf4j
@Service
public class AssetServiceImpl implements AssetService {

    private final IAccountService iAccountService;
    private final ITransactionService iTransactionService;

    public AssetServiceImpl(IAccountService iAccountService,
                            ITransactionService iTransactionService) {
        this.iAccountService = iAccountService;
        this.iTransactionService = iTransactionService;
    }

    @Override
    public AssetSummaryRespVO getSummary() {
        AssetSummaryRespVO resp = new AssetSummaryRespVO();

        // 按账户类型聚合：buy为正，sell为负
        List<AccountDO> accounts = iAccountService.list();
        Map<String, BigDecimal> typeAmountMap = new LinkedHashMap<>();

        for (String type : List.of(Constants.AccountType.PLATFORM,
                Constants.AccountType.ACCOUNT,
                Constants.AccountType.BANK,
                Constants.AccountType.CARD)) {
            typeAmountMap.put(type, BigDecimal.ZERO);
        }

        BigDecimal totalAsset = BigDecimal.ZERO;

        for (AccountDO account : accounts) {
            BigDecimal amount = calcAccountAmount(account.getId());
            String type = account.getType();
            typeAmountMap.merge(type, amount, BigDecimal::add);
            totalAsset = totalAsset.add(amount);
        }

        resp.setTotalAsset(totalAsset);

        List<AssetSummaryRespVO.AllocationItem> allocationList = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : typeAmountMap.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                AssetSummaryRespVO.AllocationItem item = new AssetSummaryRespVO.AllocationItem();
                item.setType(entry.getKey());
                item.setAmount(entry.getValue());
                if (totalAsset.compareTo(BigDecimal.ZERO) > 0) {
                    item.setRatio(entry.getValue()
                            .divide(totalAsset, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100")));
                } else {
                    item.setRatio(BigDecimal.ZERO);
                }
                allocationList.add(item);
            }
        }

        resp.setAllocationList(allocationList);
        return resp;
    }

    @Override
    public HoldingsRespVO getHoldings() {
        HoldingsRespVO resp = new HoldingsRespVO();

        List<AccountDO> accounts = iAccountService.list();
        List<HoldingsRespVO.AccountHoldings> accountHoldingsList = new ArrayList<>();

        for (AccountDO account : accounts) {
            LambdaQueryWrapper<TransactionDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TransactionDO::getAccountId, account.getId())
                    .orderByDesc(TransactionDO::getDate);
            List<TransactionDO> transactions = iTransactionService.list(wrapper);

            if (transactions.isEmpty()) {
                continue;
            }

            // 按品种聚合
            Map<String, BigDecimal> symbolAmountMap = new LinkedHashMap<>();
            Map<String, Integer> symbolCountMap = new LinkedHashMap<>();

            for (TransactionDO tx : transactions) {
                String symbol = tx.getSymbol();
                BigDecimal amount = tx.getAmount();
                boolean isBuy = Constants.TransType.BUY.equals(tx.getType());
                boolean isSell = Constants.TransType.SELL.equals(tx.getType());

                if (isBuy) {
                    symbolAmountMap.merge(symbol, amount, BigDecimal::add);
                } else if (isSell) {
                    symbolAmountMap.merge(symbol, amount.negate(), BigDecimal::add);
                } else {
                    // dividend/transfer 按原值累加
                    symbolAmountMap.merge(symbol, amount, BigDecimal::add);
                }
                symbolCountMap.merge(symbol, 1, Integer::sum);
            }

            BigDecimal accountTotal = symbolAmountMap.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<HoldingsRespVO.SymbolHolding> holdings = symbolAmountMap.entrySet().stream()
                    .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) != 0)
                    .map(e -> {
                        HoldingsRespVO.SymbolHolding sh = new HoldingsRespVO.SymbolHolding();
                        sh.setSymbol(e.getKey());
                        sh.setTotalAmount(e.getValue());
                        sh.setTransactionCount(symbolCountMap.get(e.getKey()));
                        return sh;
                    })
                    .collect(Collectors.toList());

            if (!holdings.isEmpty()) {
                HoldingsRespVO.AccountHoldings ah = new HoldingsRespVO.AccountHoldings();
                ah.setAccountId(account.getId());
                ah.setAccountName(account.getName());
                ah.setAccountType(account.getType());
                ah.setTotalAmount(accountTotal);
                ah.setHoldings(holdings);
                accountHoldingsList.add(ah);
            }
        }

        resp.setAccountHoldings(accountHoldingsList);
        return resp;
    }

    private BigDecimal calcAccountAmount(Long accountId) {
        LambdaQueryWrapper<TransactionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionDO::getAccountId, accountId);
        List<TransactionDO> txs = iTransactionService.list(wrapper);

        BigDecimal total = BigDecimal.ZERO;
        for (TransactionDO tx : txs) {
            switch (tx.getType()) {
                case Constants.TransType.BUY:
                case Constants.TransType.DIVIDEND:
                case Constants.TransType.TRANSFER:
                    total = total.add(tx.getAmount());
                    break;
                case Constants.TransType.SELL:
                    total = total.subtract(tx.getAmount());
                    break;
            }
        }
        return total;
    }
}
