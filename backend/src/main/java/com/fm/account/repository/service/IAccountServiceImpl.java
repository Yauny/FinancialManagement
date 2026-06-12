package com.fm.account.repository.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.account.repository.entity.AccountDO;
import com.fm.account.repository.mapper.AccountMapper;
import org.springframework.stereotype.Service;

/**
 * 账户数据访问服务实现
 */
@Service
public class IAccountServiceImpl extends ServiceImpl<AccountMapper, AccountDO> implements IAccountService {
}
