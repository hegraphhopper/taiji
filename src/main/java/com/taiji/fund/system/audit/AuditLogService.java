package com.taiji.fund.system.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.taiji.fund.common.api.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计查询与 CSV 导出：只读，仅管理员。
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final long MAX_PAGE_SIZE = 100L;
    private static final int EXPORT_LIMIT = 5000;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final AuditLogMapper auditLogMapper;

    @Transactional(readOnly = true)
    public PageResult<AuditLogVO> list(AuditLogQuery query) {
        AuditLogQuery actual = query == null ? new AuditLogQuery() : query;
        long pageNo = Math.max(1L, actual.getPage() == null ? 1L : actual.getPage());
        long pageSize = Math.min(MAX_PAGE_SIZE,
                Math.max(1L, actual.getSize() == null ? 10L : actual.getSize()));
        Page<AuditLog> page = auditLogMapper.selectPage(new Page<>(pageNo, pageSize),
                wrapper(actual));
        List<AuditLogVO> records = page.getRecords().stream()
                .map(AuditLogService::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public byte[] export(AuditLogQuery query) {
        LambdaQueryWrapper<AuditLog> wrapper = wrapper(query == null ? new AuditLogQuery() : query);
        wrapper.last("LIMIT " + EXPORT_LIMIT);
        List<AuditLog> logs = auditLogMapper.selectList(wrapper);
        StringBuilder builder = new StringBuilder();
        builder.append("时间,操作人,动作,资源类型,资源ID,结果,IP,耗时(ms),详情\r\n");
        for (AuditLog log : logs) {
            builder.append(cell(log.getCreatedAt() == null
                            ? "" : log.getCreatedAt().format(TIME_FORMAT))).append(',')
                    .append(cell(log.getUsername())).append(',')
                    .append(cell(log.getAction())).append(',')
                    .append(cell(log.getResourceType())).append(',')
                    .append(cell(log.getResourceId())).append(',')
                    .append(cell(log.getResult())).append(',')
                    .append(cell(log.getIp())).append(',')
                    .append(log.getCostMs() == null ? "" : log.getCostMs()).append(',')
                    .append(cell(log.getDetail())).append("\r\n");
        }
        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, content, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, content, UTF8_BOM.length, body.length);
        return content;
    }

    private LambdaQueryWrapper<AuditLog> wrapper(AuditLogQuery query) {
        LambdaQueryWrapper<AuditLog> wrapper = Wrappers.lambdaQuery();
        if (query.getFrom() != null) {
            wrapper.ge(AuditLog::getCreatedAt, query.getFrom().atStartOfDay());
        }
        if (query.getTo() != null) {
            wrapper.le(AuditLog::getCreatedAt, query.getTo().atTime(23, 59, 59));
        }
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(AuditLog::getUsername, query.getUsername().trim());
        }
        if (StringUtils.hasText(query.getAction())) {
            wrapper.like(AuditLog::getAction, query.getAction().trim());
        }
        if (StringUtils.hasText(query.getResourceType())) {
            wrapper.eq(AuditLog::getResourceType, query.getResourceType().trim());
        }
        if (StringUtils.hasText(query.getResult())) {
            wrapper.eq(AuditLog::getResult, query.getResult().trim().toUpperCase());
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt).orderByDesc(AuditLog::getId);
        return wrapper;
    }

    private static AuditLogVO toVO(AuditLog log) {
        return AuditLogVO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .detail(log.getDetail())
                .ip(log.getIp())
                .result(log.getResult())
                .costMs(log.getCostMs())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String cell(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
