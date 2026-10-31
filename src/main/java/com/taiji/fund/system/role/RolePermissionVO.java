package com.taiji.fund.system.role;

import com.taiji.fund.security.PermissionCatalog;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RolePermissionVO {

    private Long id;

    private String code;

    private String name;

    private String description;

    private List<PermissionCatalog.PermissionDef> permissions;
}
