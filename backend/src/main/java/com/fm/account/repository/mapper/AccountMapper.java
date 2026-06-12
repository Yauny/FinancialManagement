package com.fm.account.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.account.repository.entity.AccountDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户 Mapper
 */
@Mapper
public interface AccountMapper extends BaseMapper<AccountDO> {
}
