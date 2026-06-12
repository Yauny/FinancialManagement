package com.fm.transaction.controller.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易响应数据
 */
@Data
public class TransactionRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long accountId;
    private String accountName;
    private BigDecimal amount;
    private LocalDate date;
    private String symbol;
    private String type;
    private String note;
    private LocalDateTime createdAt;
}
