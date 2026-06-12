package com.fm.account.service;

import com.fm.account.controller.vo.AccountReqVO;
import com.fm.account.controller.vo.AccountRespVO;
import com.fm.account.controller.vo.AccountSaveVO;

import java.util.List;

/**
 * 账户业务服务
 */
public interface AccountService {

    List<AccountRespVO> listAccounts(AccountReqVO reqVO);

    AccountRespVO getById(Long id);

    void save(AccountSaveVO saveVO);

    void update(Long id, AccountSaveVO saveVO);

    void delete(Long id);

    /**
     * 批量删除账户（同一事务，级联软删除其下交易）
     */
    void batchDelete(List<Long> ids);
}
