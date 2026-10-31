package com.taiji.fund.system.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordResetVO {

    private Long userId;

    private String username;

    private String temporaryPassword;
}
