package com.fm.account.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 账户保存/更新请求参数
 */
@Data
public class AccountSaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，编辑时传入；新增时不传 */
    private Long id;

    @NotBlank(message = "账户名称不能为空")
    @Size(max = 100, message = "账户名称不超过100字符")
    private String name;

    @NotNull(message = "账户类型不能为空")
    private String type;

    @Size(max = 50, message = "平台来源不超过50字符")
    private String platform;
}
