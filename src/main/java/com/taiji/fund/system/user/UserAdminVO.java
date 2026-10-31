package com.taiji.fund.system.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserAdminVO {

    private Long id;

    private String username;

    private String realName;

    private String email;

    private String phone;

    private Long groupId;

    private String groupName;

    private String status;

    private List<String> roles;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;
}
