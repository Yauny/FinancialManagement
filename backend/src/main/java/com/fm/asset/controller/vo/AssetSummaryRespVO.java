package com.fm.asset.controller.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 资产总览响应数据
 */
@Data
public class AssetSummaryRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总资产 */
    private BigDecimal totalAsset;

    /** 按账户类型聚合的资产列表 */
    private List<AllocationItem> allocationList;

    @Data
    public static class AllocationItem implements Serializable {
        private String type;
        private BigDecimal amount;
        private BigDecimal ratio;
    }
}
