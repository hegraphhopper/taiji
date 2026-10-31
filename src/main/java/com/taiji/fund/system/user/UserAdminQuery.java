package com.taiji.fund.system.user;

import lombok.Data;

@Data
public class UserAdminQuery {

    private String keyword;

    private Long groupId;

    private String status;

    private String role;

    private Long page = 1L;

    private Long size = 10L;
}
