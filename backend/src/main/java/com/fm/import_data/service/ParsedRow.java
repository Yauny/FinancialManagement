package com.fm.import_data.service;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Markdown 表格解析结果
 */
@Data
public class ParsedRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String symbol;
    private String code;
    private BigDecimal amount;
    private String ratio;
    private String fee;
    private String note;
    private boolean success;
    private String errorReason;
}
