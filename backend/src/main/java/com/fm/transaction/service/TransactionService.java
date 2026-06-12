package com.fm.transaction.service;

import com.fm.transaction.controller.vo.TransactionReqVO;
import com.fm.transaction.controller.vo.TransactionRespVO;
import com.fm.transaction.controller.vo.TransactionSaveVO;
import com.fm.transaction.controller.vo.TransactionStatisticsRespVO;

import java.time.LocalDate;
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

    /**
     * 批量保存（导入的二次封装，校验后批量入库）
     */
    int batchSave(List<TransactionSaveVO> saveVOs);

    /**
     * 区间统计：按交易类型聚合
     */
    TransactionStatisticsRespVO getStatistics(LocalDate start, LocalDate end);
}
