package com.fm.asset.controller.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 资产快照响应数据（趋势图使用）
 */
@Data
public class AssetSnapshotRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDate snapshotDate;
    private BigDecimal totalAsset;
    private List<AssetSummaryRespVO.AllocationItem> allocationList;
}
