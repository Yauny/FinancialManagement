package com.fm.account.controller.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 账户响应数据
 */
@Data
public class AccountRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String type;
    private String platform;
    private LocalDateTime createdAt;
}
