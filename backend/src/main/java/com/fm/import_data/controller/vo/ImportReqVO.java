package com.fm.import_data.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Obsidian Markdown 导入请求参数
 */
@Data
public class ImportReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "账户ID不能为空")
    private Long accountId;

    @NotBlank(message = "Markdown内容不能为空")
    private String markdownContent;
}
