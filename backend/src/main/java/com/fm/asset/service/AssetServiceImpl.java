package com.fm.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.account.repository.entity.AccountDO;
import com.fm.account.repository.service.IAccountService;
import com.fm.asset.controller.vo.AssetSnapshotRespVO;
import com.fm.asset.controller.vo.AssetSummaryRespVO;
import com.fm.asset.controller.vo.HoldingsRespVO;
import com.fm.asset.service.bo.AssetSnapshotBO;
import com.fm.common.Constants;
import com.fm.common.config.CacheConfig;
import com.fm.common.event.AssetCacheEvictEvent;
import com.fm.transaction.repository.entity.TransactionDO;
import com.fm.transaction.repository.service.ITransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产总览业务服务实现
 */
@Slf4j
@Service
public class AssetServiceImpl implements AssetService {

    private static final int DEFAULT_SNAPSHOT_DAYS = 30;

    private final IAccountService iAccountService;
    private final ITransactionService iTransactionService;
    private final AssetSnapshotService assetSnapshotService;
    private final org.springframework.cache.CacheManager cacheManager;

    public AssetServiceImpl(IAccountService iAccountService,
                            ITransactionService iTransactionService,
                            AssetSnapshotService assetSnapshotService,
                            org.springframework.cache.CacheManager cacheManager) {
        this.iAccountService = iAccountService;
        this.iTransactionService = iTransactionService;
        this.assetSnapshotService = assetSnapshotService;
        this.cacheManager = cacheManager;
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_ASSET_SUMMARY, key = "'all'")
    public AssetSummaryRespVO getSummary() {
        log.info("[getSummary] 计算资产总览");

        AssetSummaryRespVO resp = new AssetSummaryRespVO();

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
    @Cacheable(value = CacheConfig.CACHE_ASSET_HOLDINGS, key = "'all'")
    public HoldingsRespVO getHoldings() {
        log.info("[getHoldings] 计算持仓明细");

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

    @Override
    public List<AssetSnapshotRespVO> getSnapshots(int days) {
        int effectiveDays = days <= 0 ? DEFAULT_SNAPSHOT_DAYS : days;
        log.info("[getSnapshots] 查询资产快照 days={}", effectiveDays);
        List<AssetSnapshotBO> bos = assetSnapshotService.getRecentSnapshots(effectiveDays);
        List<AssetSnapshotRespVO> result = new ArrayList<>(bos.size());
        for (AssetSnapshotBO bo : bos) {
            result.add(toSnapshotResp(bo));
        }
        return result;
    }

    private AssetSnapshotRespVO toSnapshotResp(AssetSnapshotBO bo) {
        AssetSnapshotRespVO vo = new AssetSnapshotRespVO();
        vo.setId(bo.getId());
        vo.setSnapshotDate(bo.getSnapshotDate());
        vo.setTotalAsset(bo.getTotalAsset());
        List<AssetSummaryRespVO.AllocationItem> items = new ArrayList<>();
        if (bo.getAllocationList() != null) {
            for (AssetSnapshotBO.AllocationItem src : bo.getAllocationList()) {
                AssetSummaryRespVO.AllocationItem item = new AssetSummaryRespVO.AllocationItem();
                item.setType(src.getType());
                item.setAmount(src.getAmount());
                item.setRatio(src.getRatio());
                items.add(item);
            }
        }
        vo.setAllocationList(items);
        return vo;
    }

    /**
     * 手动清除资产相关缓存（业务侧可通过发布事件触发，也可直接调用）
     * 直接使用 CacheManager.clear() 而非 @CacheEvict 注解，避免内部调用 AOP 不生效的问题。
     */
    public void evictAssetCaches() {
        log.info("[evictAssetCaches] 手动清除资产缓存");
        for (String name : new String[]{CacheConfig.CACHE_ASSET_SUMMARY, CacheConfig.CACHE_ASSET_HOLDINGS, CacheConfig.CACHE_ACCOUNT_LIST}) {
            org.springframework.cache.Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
                log.info("[evictAssetCaches] 已清空 cache={}", name);
            }
        }
    }

    /**
     * 监听资产缓存失效事件：交易、账户、导入变更时自动清缓存
     * 使用 @TransactionalEventListener(AFTER_COMMIT) 确保只在业务事务提交后才清缓存，
     * 避免在事务内清缓存后被同一调用链的 @Cacheable 又写入旧数据。
     */
    @org.springframework.transaction.event.TransactionalEventListener(
            phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void onAssetCacheEvictEvent(AssetCacheEvictEvent event) {
        log.info("[onAssetCacheEvictEvent] 收到资产缓存失效事件 event={}", event);
        evictAssetCaches();
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
                default:
                    break;
            }
        }
        return total;
    }
}
