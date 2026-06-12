package com.fm.account.service.bo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 账户业务对象，跨service边界返回的内部DTO
 */
@Data
public class AccountBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String type;
    private String platform;
    private LocalDateTime createdAt;
}
