package com.fm.account.controller.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 账户查询请求参数
 */
@Data
public class AccountReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String platform;
}
