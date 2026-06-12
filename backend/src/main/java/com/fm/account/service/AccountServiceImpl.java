package com.fm.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.account.controller.vo.AccountReqVO;
import com.fm.account.controller.vo.AccountRespVO;
import com.fm.account.controller.vo.AccountSaveVO;
import com.fm.account.repository.entity.AccountDO;
import com.fm.account.repository.service.IAccountService;
import com.fm.account.service.bo.AccountBO;
import com.fm.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账户业务服务实现
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

    private final IAccountService iAccountService;

    public AccountServiceImpl(IAccountService iAccountService) {
        this.iAccountService = iAccountService;
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
        BeanUtils.copyProperties(saveVO, entity);
        iAccountService.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AccountSaveVO saveVO) {
        validateType(saveVO.getType());
        AccountDO entity = iAccountService.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("账户不存在，id=" + id);
        }
        BeanUtils.copyProperties(saveVO, entity, "id");
        entity.setId(id);
        iAccountService.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        iAccountService.removeById(id);
    }

    private void validateType(String type) {
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("无效的账户类型：" + type);
        }
    }

    private AccountRespVO toRespVO(AccountDO entity) {
        AccountRespVO vo = new AccountRespVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
