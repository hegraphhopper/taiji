package com.taiji.fund.security;

import com.taiji.fund.common.enums.RoleCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一权限目录：权限点 → 允许角色。前端菜单/按钮与后端矩阵测试共用同一口径。
 * 后端接口仍以角色注解做最终校验，本目录不参与运行时鉴权。
 */
public final class PermissionCatalog {

    private static final Map<String, PermissionDef> DEFINITIONS = new LinkedHashMap<>();

    static {
        define("SYSTEM_USER_MANAGE", "用户管理", RoleCode.ADMIN);
        define("SYSTEM_GROUP_MANAGE", "项目组管理", RoleCode.ADMIN);
        define("SYSTEM_DICT_MANAGE", "技术方向管理", RoleCode.ADMIN);
        define("SYSTEM_CONFIG_MANAGE", "系统参数管理", RoleCode.ADMIN);
        define("ROLE_VIEW", "角色与权限点查看", RoleCode.ADMIN);
        define("AUDIT_VIEW", "审计日志", RoleCode.ADMIN);
        define("ANALYSIS_RUN", "分析运行", RoleCode.ADMIN, RoleCode.PMO);
        define("ANALYSIS_CONFIG", "分析配置", RoleCode.ADMIN);
        define("INSIGHT_TREND", "趋势与归因", RoleCode.ADMIN, RoleCode.PMO, RoleCode.GROUP_USER);
        define("INSIGHT_INVESTMENT", "投入态势", RoleCode.ADMIN, RoleCode.PMO, RoleCode.GROUP_USER);
        define("DECISION_PRECHECK", "辅助决策预检", RoleCode.ADMIN, RoleCode.PMO, RoleCode.GROUP_USER);
        define("DECISION_RECORD_VIEW", "决策记录查看", RoleCode.ADMIN, RoleCode.PMO,
                RoleCode.GROUP_USER, RoleCode.EXPERT);
        define("DECISION_RULE_MANAGE", "决策规则配置", RoleCode.ADMIN);
        define("PROJECT_VIEW_ALL", "全所项目查看", RoleCode.ADMIN, RoleCode.PMO);
        define("PROJECT_VIEW_GROUP", "本组项目查看", RoleCode.GROUP_USER);
        define("PROJECT_EDIT", "项目维护", RoleCode.ADMIN, RoleCode.GROUP_USER);
        define("PROJECT_SUBMIT", "项目提交与撤回", RoleCode.ADMIN, RoleCode.GROUP_USER);
        define("PROJECT_REVIEW", "形式审查与立项", RoleCode.ADMIN, RoleCode.PMO);
        define("PROJECT_EXECUTE", "执行与结项", RoleCode.ADMIN, RoleCode.GROUP_USER);
        define("PROJECT_REVIEW_SCORE", "专家评审", RoleCode.ADMIN, RoleCode.EXPERT);
        define("TODO_VIEW", "待办查看", RoleCode.ADMIN, RoleCode.PMO,
                RoleCode.GROUP_USER, RoleCode.EXPERT);
        define("REPORT_EXPORT", "报告导出", RoleCode.ADMIN, RoleCode.PMO,
                RoleCode.GROUP_USER, RoleCode.EXPERT);
    }

    private PermissionCatalog() {
    }

    public static List<PermissionDef> all() {
        return new ArrayList<>(DEFINITIONS.values());
    }

    public static Set<String> codesForRoles(List<String> roles) {
        Set<String> result = new LinkedHashSet<>();
        if (roles == null) {
            return result;
        }
        for (PermissionDef definition : DEFINITIONS.values()) {
            for (String role : definition.getRoles()) {
                if (roles.contains(role)) {
                    result.add(definition.getCode());
                    break;
                }
            }
        }
        return result;
    }

    public static List<PermissionDef> forRole(String role) {
        List<PermissionDef> result = new ArrayList<>();
        for (PermissionDef definition : DEFINITIONS.values()) {
            if (definition.getRoles().contains(role)) {
                result.add(definition);
            }
        }
        return result;
    }

    private static void define(String code, String label, RoleCode... roles) {
        List<String> names = new ArrayList<>();
        for (RoleCode role : roles) {
            names.add(role.name());
        }
        DEFINITIONS.put(code, new PermissionDef(code, label, Collections.unmodifiableList(names)));
    }

    public static final class PermissionDef {

        private final String code;

        private final String label;

        private final List<String> roles;

        private PermissionDef(String code, String label, List<String> roles) {
            this.code = code;
            this.label = label;
            this.roles = roles;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}
