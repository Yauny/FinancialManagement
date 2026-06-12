package com.fm.asset.service;

import com.fm.asset.controller.vo.AssetSummaryRespVO;
import com.fm.asset.controller.vo.HoldingsRespVO;

/**
 * 资产总览业务服务
 */
public interface AssetService {

    AssetSummaryRespVO getSummary();

    HoldingsRespVO getHoldings();
}
