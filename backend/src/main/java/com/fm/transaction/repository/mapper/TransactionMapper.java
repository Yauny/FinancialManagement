package com.fm.transaction.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.transaction.repository.entity.TransactionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交易 Mapper
 */
@Mapper
public interface TransactionMapper extends BaseMapper<TransactionDO> {
}
