package com.fm.transaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.account.repository.entity.AccountDO;
import com.fm.account.repository.service.IAccountService;
import com.fm.transaction.controller.vo.TransactionReqVO;
import com.fm.transaction.controller.vo.TransactionRespVO;
import com.fm.transaction.controller.vo.TransactionSaveVO;
import com.fm.transaction.repository.entity.TransactionDO;
import com.fm.transaction.repository.service.ITransactionService;
import com.fm.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 交易业务服务实现
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

    public TransactionServiceImpl(ITransactionService iTransactionService,
                                  IAccountService iAccountService) {
        this.iTransactionService = iTransactionService;
        this.iAccountService = iAccountService;
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
        return iTransactionService.list(wrapper).stream()
                .map(this::toRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionRespVO getById(Long id) {
        TransactionDO entity = iTransactionService.getById(id);
        return entity == null ? null : toRespVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(TransactionSaveVO saveVO) {
        validateType(saveVO.getType());
        validateAccount(saveVO.getAccountId());
        TransactionDO entity = new TransactionDO();
        BeanUtils.copyProperties(saveVO, entity);
        iTransactionService.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TransactionSaveVO saveVO) {
        validateType(saveVO.getType());
        validateAccount(saveVO.getAccountId());
        TransactionDO entity = iTransactionService.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("交易记录不存在，id=" + id);
        }
        BeanUtils.copyProperties(saveVO, entity, "id");
        entity.setId(id);
        iTransactionService.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        iTransactionService.removeById(id);
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

    private TransactionRespVO toRespVO(TransactionDO entity) {
        TransactionRespVO vo = new TransactionRespVO();
        BeanUtils.copyProperties(entity, vo);
        AccountDO account = iAccountService.getById(entity.getAccountId());
        if (account != null) {
            vo.setAccountName(account.getName());
        }
        return vo;
    }
}
