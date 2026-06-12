package com.fm.transaction.service.bo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易业务对象，跨service边界返回的内部DTO
 */
@Data
public class TransactionBO implements Serializable {

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
