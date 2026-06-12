package com.fm.transaction.controller.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 交易查询请求参数
 */
@Data
public class TransactionReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accountId;
    private String symbol;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
}
