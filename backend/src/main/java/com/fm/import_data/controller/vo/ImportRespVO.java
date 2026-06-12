package com.fm.import_data.controller.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 导入结果响应
 */
@Data
public class ImportRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalRows;
    private int successCount;
    private int failCount;
    private String message;
}
