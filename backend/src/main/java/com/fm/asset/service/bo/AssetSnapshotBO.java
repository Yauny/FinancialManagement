package com.fm.asset.service.bo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 资产快照业务对象
 */
@Data
public class AssetSnapshotBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDate snapshotDate;
    private BigDecimal totalAsset;
    private List<AllocationItem> allocationList;

    @Data
    public static class AllocationItem implements Serializable {
        private String type;
        private BigDecimal amount;
        private BigDecimal ratio;
    }
}
