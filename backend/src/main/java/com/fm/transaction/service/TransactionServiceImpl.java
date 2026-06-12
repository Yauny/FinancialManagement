package com.fm.transaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.account.repository.entity.AccountDO;
import com.fm.account.repository.service.IAccountService;
import com.fm.common.Constants;
import com.fm.common.event.AssetCacheEvictEvent;
import com.fm.transaction.controller.vo.TransactionReqVO;
import com.fm.transaction.controller.vo.TransactionRespVO;
import com.fm.transaction.controller.vo.TransactionSaveVO;
import com.fm.transaction.controller.vo.TransactionStatisticsRespVO;
import com.fm.transaction.repository.entity.TransactionDO;
import com.fm.transaction.repository.service.ITransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 交易业务服务实现
 *
 * 不可变性：所有 DO → VO 转换均使用手动字段复制，不调用 BeanUtils
 * 批量保存：每条 saveVO 独立校验失败不影响整批（错误信息通过异常抛出）
 */
@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {

    private static final List<String> ALLOWED_TYPES = List.of(
            Constants.TransType.BUY,
            Constants.TransType.SELL,
            Constants.TransType.DIVIDEND,
            Constants.TransType.TRANSFER
    );

    private final ITransactionService iTransactionService;
    private final IAccountService iAccountService;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionServiceImpl(ITransactionService iTransactionService,
                                  IAccountService iAccountService,
                                  ApplicationEventPublisher eventPublisher) {
        this.iTransactionService = iTransactionService;
        this.iAccountService = iAccountService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 业务变更后发布资产缓存失效事件
     */
    private void publishAssetCacheEvict(String reason) {
        eventPublisher.publishEvent(new AssetCacheEvictEvent("TransactionService", reason));
    }

    @Override
    public List<TransactionRespVO> listTransactions(TransactionReqVO reqVO) {
        LambdaQueryWrapper<TransactionDO> wrapper = new LambdaQueryWrapper<>();
        if (reqVO != null) {
            if (reqVO.getAccountId() != null) {
                wrapper.eq(TransactionDO::getAccountId, reqVO.getAccountId());
            }
            if (reqVO.getSymbol() != null) {
                wrapper.like(TransactionDO::getSymbol, reqVO.getSymbol());
            }
            if (reqVO.getType() != null) {
                wrapper.eq(TransactionDO::getType, reqVO.getType());
            }
            if (reqVO.getStartDate() != null) {
                wrapper.ge(TransactionDO::getDate, reqVO.getStartDate());
            }
            if (reqVO.getEndDate() != null) {
                wrapper.le(TransactionDO::getDate, reqVO.getEndDate());
            }
        }
        wrapper.orderByDesc(TransactionDO::getDate);

        List<TransactionDO> entities = iTransactionService.list(wrapper);
        if (entities.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量加载账户名，避免 N+1
        List<Long> accountIds = entities.stream()
                .map(TransactionDO::getAccountId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> accountNameMap = new HashMap<>();
        if (!accountIds.isEmpty()) {
            for (AccountDO a : iAccountService.listByIds(accountIds)) {
                accountNameMap.put(a.getId(), a.getName());
            }
        }

        List<TransactionRespVO> result = new ArrayList<>(entities.size());
        for (TransactionDO e : entities) {
            result.add(toRespVO(e, accountNameMap));
        }
        return result;
    }

    @Override
    public TransactionRespVO getById(Long id) {
        TransactionDO entity = iTransactionService.getById(id);
        if (entity == null) {
            return null;
        }
        return toRespVO(entity, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(TransactionSaveVO saveVO) {
        validate(saveVO);
        TransactionDO entity = toDO(saveVO);
        iTransactionService.save(entity);
        publishAssetCacheEvict(AssetCacheEvictEvent.REASON_TRANSACTION_CHANGE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TransactionSaveVO saveVO) {
        validate(saveVO);
        TransactionDO existing = iTransactionService.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("交易记录不存在，id=" + id);
        }
        TransactionDO entity = toDO(saveVO);
        entity.setId(id);
        iTransactionService.updateById(entity);
        publishAssetCacheEvict(AssetCacheEvictEvent.REASON_TRANSACTION_CHANGE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (iTransactionService.getById(id) == null) {
            throw new IllegalArgumentException("交易记录不存在，id=" + id);
        }
        iTransactionService.removeById(id);
        publishAssetCacheEvict(AssetCacheEvictEvent.REASON_TRANSACTION_CHANGE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSave(List<TransactionSaveVO> saveVOs) {
        if (saveVOs == null || saveVOs.isEmpty()) {
            throw new IllegalArgumentException("批量保存的交易列表不能为空");
        }
        List<TransactionDO> entities = new ArrayList<>(saveVOs.size());
        for (int i = 0; i < saveVOs.size(); i++) {
            TransactionSaveVO vo = saveVOs.get(i);
            try {
                validate(vo);
                entities.add(toDO(vo));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "第 " + (i + 1) + " 条校验失败：" + e.getMessage(), e);
            }
        }
        iTransactionService.saveBatch(entities);
        publishAssetCacheEvict(AssetCacheEvictEvent.REASON_TRANSACTION_CHANGE);
        return entities.size();
    }

    @Override
    public TransactionStatisticsRespVO getStatistics(LocalDate start, LocalDate end) {
        if (start == null) {
            start = LocalDate.now().minusMonths(1);
        }
        if (end == null) {
            end = LocalDate.now();
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        LambdaQueryWrapper<TransactionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(TransactionDO::getDate, start)
                .le(TransactionDO::getDate, end);
        List<TransactionDO> txs = iTransactionService.list(wrapper);

        TransactionStatisticsRespVO resp = new TransactionStatisticsRespVO();
        resp.setStartDate(start);
        resp.setEndDate(end);
        resp.setTotalCount(txs.size());
        resp.setTotalAmount(BigDecimal.ZERO);
        resp.setBuyAmount(BigDecimal.ZERO);
        resp.setSellAmount(BigDecimal.ZERO);
        resp.setDividendAmount(BigDecimal.ZERO);
        resp.setTransferAmount(BigDecimal.ZERO);

        for (TransactionDO tx : txs) {
            BigDecimal amount = tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount();
            resp.setTotalAmount(resp.getTotalAmount().add(amount));
            String type = tx.getType();
            if (Constants.TransType.BUY.equals(type)) {
                resp.setBuyAmount(resp.getBuyAmount().add(amount));
                resp.setBuyCount(resp.getBuyCount() + 1);
            } else if (Constants.TransType.SELL.equals(type)) {
                resp.setSellAmount(resp.getSellAmount().add(amount));
                resp.setSellCount(resp.getSellCount() + 1);
            } else if (Constants.TransType.DIVIDEND.equals(type)) {
                resp.setDividendAmount(resp.getDividendAmount().add(amount));
                resp.setDividendCount(resp.getDividendCount() + 1);
            } else if (Constants.TransType.TRANSFER.equals(type)) {
                resp.setTransferAmount(resp.getTransferAmount().add(amount));
                resp.setTransferCount(resp.getTransferCount() + 1);
            }
        }
        return resp;
    }

    private void validate(TransactionSaveVO saveVO) {
        validateType(saveVO.getType());
        validateAccount(saveVO.getAccountId());
        if (saveVO.getAmount() == null) {
            throw new IllegalArgumentException("金额不能为空");
        }
        if (saveVO.getDate() == null) {
            throw new IllegalArgumentException("交易日期不能为空");
        }
        if (saveVO.getSymbol() == null || saveVO.getSymbol().isBlank()) {
            throw new IllegalArgumentException("品种名称不能为空");
        }
    }

    private void validateType(String type) {
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("无效的交易类型：" + type);
        }
    }

    private void validateAccount(Long accountId) {
        if (accountId == null || iAccountService.getById(accountId) == null) {
            throw new IllegalArgumentException("账户不存在，accountId=" + accountId);
        }
    }

    private TransactionDO toDO(TransactionSaveVO vo) {
        TransactionDO entity = new TransactionDO();
        entity.setAccountId(vo.getAccountId());
        entity.setAmount(vo.getAmount());
        entity.setDate(vo.getDate());
        entity.setSymbol(vo.getSymbol());
        entity.setType(vo.getType());
        entity.setNote(vo.getNote());
        return entity;
    }

    private TransactionRespVO toRespVO(TransactionDO entity, Map<Long, String> accountNameMap) {
        TransactionRespVO vo = new TransactionRespVO();
        vo.setId(entity.getId());
        vo.setAccountId(entity.getAccountId());
        vo.setAmount(entity.getAmount());
        vo.setDate(entity.getDate());
        vo.setSymbol(entity.getSymbol());
        vo.setType(entity.getType());
        vo.setNote(entity.getNote());
        vo.setCreatedAt(entity.getCreatedAt());
        if (accountNameMap != null) {
            vo.setAccountName(accountNameMap.get(entity.getAccountId()));
        } else {
            AccountDO account = iAccountService.getById(entity.getAccountId());
            if (account != null) {
                vo.setAccountName(account.getName());
            }
        }
        return vo;
    }
}
