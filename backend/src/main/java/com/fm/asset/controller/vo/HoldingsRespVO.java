package com.fm.asset.controller.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 持仓明细响应数据
 */
@Data
public class HoldingsRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<AccountHoldings> accountHoldings;

    @Data
    public static class AccountHoldings implements Serializable {
        private Long accountId;
        private String accountName;
        private String accountType;
        private BigDecimal totalAmount;
        private List<SymbolHolding> holdings;
    }

    @Data
    public static class SymbolHolding implements Serializable {
        private String symbol;
        private BigDecimal totalAmount;
        private Integer transactionCount;
    }
}
