package com.taiji.fund.system.user;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class UserSaveRequest {

    @NotBlank
    @Size(max = 50)
    private String username;

    @Size(min = 8, max = 64)
    private String password;

    @Size(max = 50)
    private String realName;

    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phone;

    private Long groupId;

    private String status;

    private List<String> roles;
}
