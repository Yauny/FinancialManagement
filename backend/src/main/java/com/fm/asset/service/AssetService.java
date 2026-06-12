package com.fm.asset.service;

import com.fm.asset.controller.vo.AssetSnapshotRespVO;
import com.fm.asset.controller.vo.AssetSummaryRespVO;
import com.fm.asset.controller.vo.HoldingsRespVO;

import java.util.List;

/**
 * 资产总览业务服务
 */
public interface AssetService {

    AssetSummaryRespVO getSummary();

    HoldingsRespVO getHoldings();

    /**
     * 查询最近 N 天的资产快照（趋势图用）
     *
     * @param days 天数，必须 > 0
     * @return 快照列表（按日期升序）
     */
    List<AssetSnapshotRespVO> getSnapshots(int days);
}
