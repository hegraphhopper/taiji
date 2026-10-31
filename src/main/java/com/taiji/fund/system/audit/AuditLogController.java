package com.taiji.fund.system.audit;

import com.taiji.fund.common.api.PageResult;
import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 审计日志查询与导出：仅管理员。
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AuditLogService auditLogService;

    @GetMapping
    public Result<PageResult<AuditLogVO>> list(AuditLogQuery query) {
        return Result.ok(auditLogService.list(query));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(AuditLogQuery query) {
        byte[] content = auditLogService.export(query);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("audit-logs-" + LocalDate.now().format(FILE_DATE) + ".csv",
                        StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }
}
