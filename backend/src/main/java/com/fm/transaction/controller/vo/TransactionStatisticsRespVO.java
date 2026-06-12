package com.fm.transaction.controller.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 交易统计响应数据
 */
@Data
public class TransactionStatisticsRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long totalCount;
    private BigDecimal totalAmount;
    private BigDecimal buyAmount;
    private BigDecimal sellAmount;
    private BigDecimal dividendAmount;
    private BigDecimal transferAmount;
    private long buyCount;
    private long sellCount;
    private long dividendCount;
    private long transferCount;
    private LocalDate startDate;
    private LocalDate endDate;
}
