package com.taiji.fund.common.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taiji.fund.security.LoginUser;
import com.taiji.fund.security.SecurityUtils;
import com.taiji.fund.security.dto.LoginRequest;
import com.taiji.fund.system.audit.AuditLog;
import com.taiji.fund.system.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final int MAX_DETAIL_LENGTH = 1900;

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Around(value = "@annotation(audit)", argNames = "joinPoint,audit")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long start = System.currentTimeMillis();
        String result = "SUCCESS";
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            result = "FAIL";
            throw throwable;
        } finally {
            long cost = System.currentTimeMillis() - start;
            try {
                saveAudit(joinPoint, audit, result, cost);
            } catch (Exception ex) {
                log.warn("审计日志写入失败: action={}, resourceType={}", audit.action(), audit.resourceType(), ex);
            }
        }
    }

    private void saveAudit(ProceedingJoinPoint joinPoint, Audit audit, String result, long cost) {
        AuditLog auditLog = new AuditLog();
        LoginUser currentUser = SecurityUtils.currentUserOrNull();
        if (currentUser != null) {
            auditLog.setUserId(currentUser.getId());
            auditLog.setUsername(currentUser.getUsername());
        } else {
            auditLog.setUsername(resolveUsername(joinPoint.getArgs()));
        }
        auditLog.setAction(audit.action());
        auditLog.setResourceType(audit.resourceType());
        auditLog.setDetail(buildDetail(joinPoint.getArgs()));
        auditLog.setIp(resolveIp());
        auditLog.setResult(result);
        auditLog.setCostMs(cost);
        auditService.record(auditLog);
    }

    private String resolveUsername(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof LoginRequest) {
                return ((LoginRequest) arg).getUsername();
            }
        }
        return null;
    }

    private String buildDetail(Object[] args) {
        List<Object> payload = Arrays.stream(args)
                .filter(this::isSerializable)
                .collect(Collectors.toList());
        try {
            JsonNode node = objectMapper.valueToTree(payload);
            mask(node);
            String json = objectMapper.writeValueAsString(node);
            return json.length() > MAX_DETAIL_LENGTH ? json.substring(0, MAX_DETAIL_LENGTH) : json;
        } catch (Exception ex) {
            return "[]";
        }
    }

    private boolean isSerializable(Object arg) {
        return arg != null && !(arg instanceof ServletRequest) && !(arg instanceof ServletResponse);
    }

    private void mask(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> sensitiveFields = new ArrayList<>();
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (isSensitive(fieldName)) {
                    sensitiveFields.add(fieldName);
                }
            }
            for (String field : sensitiveFields) {
                objectNode.put(field, "***");
            }
            Iterator<JsonNode> children = objectNode.elements();
            while (children.hasNext()) {
                mask(children.next());
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                mask(child);
            }
        }
    }

    private boolean isSensitive(String fieldName) {
        String lower = fieldName.toLowerCase();
        return lower.contains("password") || lower.contains("token") || lower.contains("secret")
                || lower.contains("apikey") || lower.contains("credential") || lower.contains("authorization");
    }

    private String resolveIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
