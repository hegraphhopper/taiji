package com.taiji.fund.project.controller;

import com.taiji.fund.common.api.Result;
import com.taiji.fund.project.dto.ProjectDocumentVO;
import com.taiji.fund.project.dto.SectionPreviewVO;
import com.taiji.fund.project.service.ProjectDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectDocumentController {

    private final ProjectDocumentService documentService;

    @PostMapping("/projects/{projectId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','GROUP_USER')")
    public Result<ProjectDocumentVO> upload(@PathVariable Long projectId,
                                            @RequestParam("file") MultipartFile file) {
        return Result.ok(documentService.upload(projectId, file));
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        ProjectDocumentService.DownloadFile file = documentService.download(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.getResource());
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','GROUP_USER')")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.ok();
    }

    @PostMapping("/documents/{id}/parse")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','GROUP_USER')")
    public Result<ProjectDocumentVO> parse(@PathVariable Long id) {
        return Result.ok(documentService.parse(id));
    }

    @GetMapping("/projects/{projectId}/documents/{documentId}/sections")
    public Result<SectionPreviewVO> preview(@PathVariable Long projectId,
                                            @PathVariable Long documentId) {
        return Result.ok(documentService.preview(projectId, documentId));
    }
}
