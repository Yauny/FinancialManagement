package com.fm.transaction.repository.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.transaction.repository.entity.TransactionDO;
import com.fm.transaction.repository.mapper.TransactionMapper;
import org.springframework.stereotype.Service;

/**
 * 交易数据访问服务实现
 */
@Service
public class ITransactionServiceImpl extends ServiceImpl<TransactionMapper, TransactionDO> implements ITransactionService {
}
