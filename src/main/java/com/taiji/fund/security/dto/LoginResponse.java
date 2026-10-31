package com.taiji.fund.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private LocalDateTime expiresAt;

    private UserInfoVO user;

    private List<String> roles;

    private List<String> permissions;
}
