package com.fm.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.account.controller.vo.AccountReqVO;
import com.fm.account.controller.vo.AccountRespVO;
import com.fm.account.controller.vo.AccountSaveVO;
import com.fm.account.repository.entity.AccountDO;
import com.fm.account.repository.service.IAccountService;
import com.fm.common.Constants;
import com.fm.common.event.AssetCacheEvictEvent;
import com.fm.transaction.repository.entity.TransactionDO;
import com.fm.transaction.repository.service.ITransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账户业务服务实现
 *
 * 不可变性：所有 DO → VO 转换均使用手动字段复制，不调用 BeanUtils
 * 批量删除：级联软删除账户下所有交易（同一事务）
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    private static final List<String> ALLOWED_TYPES = List.of(
            Constants.AccountType.PLATFORM,
            Constants.AccountType.ACCOUNT,
            Constants.AccountType.BANK,
            Constants.AccountType.CARD
    );

    private static final int BATCH_DELETE_LIMIT = 100;

    private final IAccountService iAccountService;
    private final ITransactionService iTransactionService;
    private final ApplicationEventPublisher eventPublisher;

    public AccountServiceImpl(IAccountService iAccountService,
                              ITransactionService iTransactionService,
                              ApplicationEventPublisher eventPublisher) {
        this.iAccountService = iAccountService;
        this.iTransactionService = iTransactionService;
        this.eventPublisher = eventPublisher;
    }

    private void publishAssetCacheEvict(String reason) {
        eventPublisher.publishEvent(new AssetCacheEvictEvent("AccountService", reason));
    }

    @Override
    public List<AccountRespVO> listAccounts(AccountReqVO reqVO) {
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<>();
        if (reqVO != null && reqVO.getType() != null) {
            wrapper.eq(AccountDO::getType, reqVO.getType());
        }
        if (reqVO != null && reqVO.getPlatform() != null) {
            wrapper.like(AccountDO::getPlatform, reqVO.getPlatform());
        }
        wrapper.orderByDesc(AccountDO::getCreatedAt);
        return iAccountService.list(wrapper).stream()
                .map(this::toRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public AccountRespVO getById(Long id) {
        AccountDO entity = iAccountService.getById(id);
        return entity == null ? null : toRespVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(AccountSaveVO saveVO) {
        validateType(saveVO.getType());
        AccountDO entity = new AccountDO();
        entity.setName(saveVO.getName());
        entity.setType(saveVO.getType());
        entity.setPlatform(saveVO.getPlatform() == null ? "" : saveVO.getPlatform());
        iAccountService.save(entity);
        publishAssetCacheEvict(AssetCacheEvictEvent.REASON_ACCOUNT_CHANGE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AccountSaveVO saveVO) {
        validateType(saveVO.getType());
        AccountDO existing = iAccountService.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("账户不存在，id=" + id);
        }
        AccountDO entity = new AccountDO();
        entity.setId(id);
        entity.setName(saveVO.getName());
        entity.setType(saveVO.getType());
        entity.setPlatform(saveVO.getPlatform() == null ? "" : saveVO.getPlatform());
        iAccountService.updateById(entity);
        publishAssetCacheEvict(AssetCacheEvictEvent.REASON_ACCOUNT_CHANGE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (iAccountService.getById(id) == null) {
            throw new IllegalArgumentException("账户不存在，id=" + id);
        }
        LambdaQueryWrapper<TransactionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionDO::getAccountId, id);
        List<TransactionDO> transactions = iTransactionService.list(wrapper);
        for (TransactionDO tx : transactions) {
            iTransactionService.removeById(tx.getId());
        }
        iAccountService.removeById(id);
        publishAssetCacheEvict(AssetCacheEvictEvent.REASON_ACCOUNT_CHANGE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("待删除账户ID列表不能为空");
        }
        List<Long> distinctIds = ids.stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            throw new IllegalArgumentException("待删除账户ID列表不能为空");
        }

        List<Long> existingIds = iAccountService.listByIds(distinctIds).stream()
                .map(AccountDO::getId)
                .collect(Collectors.toList());
        if (existingIds.isEmpty()) {
            throw new IllegalArgumentException("所选账户均不存在");
        }

        // 分批删除交易
        List<List<Long>> partitions = partition(existingIds, BATCH_DELETE_LIMIT);
        for (List<Long> part : partitions) {
            LambdaQueryWrapper<TransactionDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(TransactionDO::getAccountId, part);
            List<TransactionDO> transactions = iTransactionService.list(wrapper);
            for (TransactionDO tx : transactions) {
                iTransactionService.removeById(tx.getId());
            }
        }

        // 分批删除账户
        for (List<Long> part : partitions) {
            iAccountService.removeByIds(part);
        }
        publishAssetCacheEvict(AssetCacheEvictEvent.REASON_ACCOUNT_CHANGE);
    }

    private void validateType(String type) {
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("无效的账户类型：" + type);
        }
    }

    private AccountRespVO toRespVO(AccountDO entity) {
        AccountRespVO vo = new AccountRespVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setPlatform(entity.getPlatform());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        if (list == null || list.isEmpty() || size <= 0) {
            return Collections.emptyList();
        }
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            int end = Math.min(i + size, list.size());
            result.add(new ArrayList<>(list.subList(i, end)));
        }
        return result;
    }
}
