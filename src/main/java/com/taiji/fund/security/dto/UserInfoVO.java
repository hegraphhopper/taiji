package com.taiji.fund.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO {

    private Long id;

    private String username;

    private String realName;

    private String email;

    private String phone;

    private Long groupId;

    private String groupName;

    private List<String> roles;

    private String status;

    private LocalDateTime lastLoginAt;
}
