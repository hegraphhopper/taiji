package com.taiji.fund.decision;

import com.taiji.fund.common.api.PageResult;
import com.taiji.fund.common.api.Result;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.decision.dto.DecisionPrecheckRequest;
import com.taiji.fund.decision.dto.DecisionPrecheckVO;
import com.taiji.fund.decision.dto.DecisionRecordQuery;
import com.taiji.fund.decision.dto.DecisionRecordVO;
import com.taiji.fund.decision.dto.DecisionRuleItem;
import com.taiji.fund.decision.dto.DecisionSaveRequest;
import com.taiji.fund.decision.report.DecisionExcelReportBuilder;
import com.taiji.fund.decision.report.DecisionWordReportBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 辅助决策接口：预检、决策记录、规则与报告导出。
 */
@RestController
@RequestMapping("/api/v1/decision")
@RequiredArgsConstructor
public class DecisionController {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DecisionPrecheckService precheckService;
    private final DecisionRecordService recordService;
    private final DecisionRuleService ruleService;
    private final DecisionWordReportBuilder wordReportBuilder;
    private final DecisionExcelReportBuilder excelReportBuilder;

    @PostMapping("/precheck")
    @PreAuthorize("hasAnyRole('GROUP_USER','PMO','ADMIN')")
    public Result<DecisionPrecheckVO> precheck(@RequestBody DecisionPrecheckRequest request) {
        return Result.ok(precheckService.precheck(request));
    }

    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('GROUP_USER','PMO','ADMIN')")
    public Result<DecisionRecordVO> save(@RequestBody DecisionSaveRequest request) {
        return Result.ok(recordService.save(request));
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('GROUP_USER','PMO','ADMIN','EXPERT')")
    public Result<PageResult<DecisionRecordVO>> list(DecisionRecordQuery query) {
        return Result.ok(recordService.list(query));
    }

    @GetMapping("/records/{id}")
    @PreAuthorize("hasAnyRole('GROUP_USER','PMO','ADMIN','EXPERT')")
    public Result<DecisionRecordVO> detail(@PathVariable Long id) {
        return Result.ok(recordService.detail(id));
    }

    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasAnyRole('PMO','ADMIN')")
    public Result<Void> voidRecord(@PathVariable Long id) {
        recordService.voidRecord(id);
        return Result.ok();
    }

    @GetMapping("/records/{id}/report.docx")
    @PreAuthorize("hasAnyRole('GROUP_USER','PMO','ADMIN','EXPERT')")
    @Audit(action = "DECISION_REPORT_EXPORT", resourceType = "DECISION_RECORD")
    public ResponseEntity<byte[]> word(@PathVariable Long id) {
        DecisionRecordVO record = recordService.detail(id);
        return download(wordReportBuilder.build(record),
                "决策报告_" + codePart(record) + "_" + LocalDate.now().format(FILE_DATE) + ".docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @GetMapping("/records/{id}/report.xlsx")
    @PreAuthorize("hasAnyRole('GROUP_USER','PMO','ADMIN','EXPERT')")
    @Audit(action = "DECISION_REPORT_EXPORT", resourceType = "DECISION_RECORD")
    public ResponseEntity<byte[]> excel(@PathVariable Long id) {
        DecisionRecordVO record = recordService.detail(id);
        return download(excelReportBuilder.build(record),
                "决策明细_" + codePart(record) + "_" + LocalDate.now().format(FILE_DATE) + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<DecisionRule>> rules() {
        return Result.ok(ruleService.list());
    }

    @PutMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<DecisionRule>> saveRules(@RequestBody List<DecisionRuleItem> items) {
        return Result.ok(ruleService.save(items));
    }

    private ResponseEntity<byte[]> download(byte[] content, String fileName, String contentType) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }

    private String codePart(DecisionRecordVO record) {
        return StringUtils.hasText(record.getProjectCode()) ? record.getProjectCode() : "TEXT";
    }
}
