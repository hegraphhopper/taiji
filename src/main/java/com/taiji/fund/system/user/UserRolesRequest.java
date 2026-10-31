package com.taiji.fund.system.user;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class UserRolesRequest {

    @NotNull
    private List<String> roles;
}
