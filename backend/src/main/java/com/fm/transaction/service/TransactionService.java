package com.fm.transaction.service;

import com.fm.transaction.controller.vo.TransactionReqVO;
import com.fm.transaction.controller.vo.TransactionRespVO;
import com.fm.transaction.controller.vo.TransactionSaveVO;

import java.util.List;

/**
 * 交易业务服务
 */
public interface TransactionService {

    List<TransactionRespVO> listTransactions(TransactionReqVO reqVO);

    TransactionRespVO getById(Long id);

    void save(TransactionSaveVO saveVO);

    void update(Long id, TransactionSaveVO saveVO);

    void delete(Long id);
}
