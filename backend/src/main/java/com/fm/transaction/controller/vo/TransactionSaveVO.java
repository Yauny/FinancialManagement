package com.fm.transaction.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 交易保存/更新请求参数
 */
@Data
public class TransactionSaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，编辑时传入；新增时不传 */
    private Long id;

    @NotNull(message = "账户ID不能为空")
    private Long accountId;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @NotNull(message = "交易日期不能为空")
    private LocalDate date;

    @NotBlank(message = "品种名称不能为空")
    @Size(max = 100, message = "品种名称不超过100字符")
    private String symbol;

    @NotNull(message = "交易类型不能为空")
    private String type;

    private String note;
}
