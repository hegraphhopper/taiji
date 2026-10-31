package com.taiji.fund.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.enums.ParseStatus;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.document.DocumentParseException;
import com.taiji.fund.document.DocumentTextExtractor;
import com.taiji.fund.document.SectionParser;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectDocument;
import com.taiji.fund.project.dto.ProjectDocumentVO;
import com.taiji.fund.project.dto.SectionPreviewVO;
import com.taiji.fund.project.mapper.ProjectDocumentMapper;
import com.taiji.fund.project.mapper.ProjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectDocumentService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final int MAX_DOCUMENTS = 10;
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(
            Arrays.asList("docx", "pdf", "txt", "md"));

    private final ProjectMapper projectMapper;
    private final ProjectDocumentMapper documentMapper;
    private final DocumentStorageService storageService;
    private final DocumentTextExtractor textExtractor;
    private final SectionParser sectionParser;
    private final ProjectAccessService accessService;

    @Audit(action = "PROJECT_DOCUMENT_UPLOAD", resourceType = "PROJECT_DOCUMENT")
    public ProjectDocumentVO upload(Long projectId, MultipartFile file) {
        Project project = loadProject(projectId);
        accessService.assertCanEdit(project);
        assertEditable(project);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请选择要上传的附件");
        }
        String fileName = cleanFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        if (!ALLOWED_TYPES.contains(extension)) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }
        long existing = documentMapper.selectCount(Wrappers.<ProjectDocument>lambdaQuery()
                .eq(ProjectDocument::getProjectId, projectId));
        if (existing >= MAX_DOCUMENTS) {
            throw new BizException(ErrorCode.FILE_LIMIT_EXCEEDED);
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "读取附件失败");
        }
        validateSignature(content, extension);
        DocumentStorageService.StoredFile stored = storageService.store(content, extension);

        ProjectDocument document = new ProjectDocument();
        document.setProjectId(projectId);
        document.setFileName(fileName);
        document.setStoredName(stored.getStoredName());
        document.setFileType(extension);
        document.setFileSize((long) content.length);
        document.setStoragePath(stored.getRelativePath());
        document.setParseStatus(ParseStatus.PENDING.getCode());
        document.setUploadedBy(accessService.currentUserId());
        document.setCreatedAt(LocalDateTime.now());
        documentMapper.insert(document);
        parseAndUpdate(document);
        return toVO(document);
    }

    @Audit(action = "PROJECT_DOCUMENT_PARSE", resourceType = "PROJECT_DOCUMENT")
    public ProjectDocumentVO parse(Long id) {
        ProjectDocument document = loadDocument(id);
        Project project = loadProject(document.getProjectId());
        accessService.assertCanEdit(project);
        assertEditable(project);
        parseAndUpdate(document);
        return toVO(document);
    }

    public SectionPreviewVO preview(Long projectId, Long documentId) {
        ProjectDocument document = loadDocumentForProject(projectId, documentId);
        String text = document.getTextContent();
        if (text == null || text.trim().isEmpty()) {
            try {
                text = textExtractor.extract(storageService.readAll(document.getStoragePath()),
                        document.getFileType());
            } catch (DocumentParseException exception) {
                SectionPreviewVO preview = new SectionPreviewVO();
                preview.setParsed(false);
                preview.setMessage(exception.getMessage());
                return preview;
            }
        }
        return sectionParser.parse(text);
    }

    private void parseAndUpdate(ProjectDocument document) {
        try {
            String text = textExtractor.extract(storageService.readAll(document.getStoragePath()),
                    document.getFileType());
            document.setTextContent(text);
            document.setParseStatus(ParseStatus.PARSED.getCode());
            document.setParseMessage(null);
        } catch (DocumentParseException exception) {
            document.setTextContent(null);
            document.setParseStatus(ParseStatus.FAILED.getCode());
            document.setParseMessage(truncate(exception.getMessage()));
        } catch (RuntimeException exception) {
            document.setTextContent(null);
            document.setParseStatus(ParseStatus.FAILED.getCode());
            document.setParseMessage("附件解析失败，请检查文件格式");
        }
        documentMapper.updateById(document);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    @Transactional(readOnly = true)
    public DownloadFile download(Long id) {
        ProjectDocument document = loadDocument(id);
        Project project = loadProject(document.getProjectId());
        accessService.assertCanView(project);
        Resource resource = storageService.load(document.getStoragePath());
        return new DownloadFile(resource, document.getFileName(), contentType(document.getFileType()));
    }

    @Transactional
    @Audit(action = "PROJECT_DOCUMENT_DELETE", resourceType = "PROJECT_DOCUMENT")
    public void delete(Long id) {
        ProjectDocument document = loadDocument(id);
        Project project = loadProject(document.getProjectId());
        accessService.assertCanEdit(project);
        assertEditable(project);
        storageService.delete(document.getStoragePath());
        documentMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ProjectDocument loadDocumentForProject(Long projectId, Long documentId) {
        ProjectDocument document = loadDocument(documentId);
        if (!projectId.equals(document.getProjectId())) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        Project project = loadProject(projectId);
        accessService.assertCanView(project);
        return document;
    }

    public ProjectDocument loadDocument(Long id) {
        ProjectDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        return document;
    }

    public ProjectDocumentVO toVO(ProjectDocument document) {
        ProjectDocumentVO vo = new ProjectDocumentVO();
        vo.setId(document.getId());
        vo.setProjectId(document.getProjectId());
        vo.setFileName(document.getFileName());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setParseStatus(document.getParseStatus());
        vo.setParseMessage(document.getParseMessage());
        vo.setUploadedBy(document.getUploadedBy());
        vo.setCreatedAt(document.getCreatedAt());
        return vo;
    }

    private Project loadProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    private void assertEditable(Project project) {
        if (!ProjectStatus.DRAFT.getCode().equals(project.getStatus())
                && !ProjectStatus.REJECTED.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.PROJECT_NOT_EDITABLE);
        }
    }

    private String cleanFileName(String original) {
        if (!StringUtils.hasText(original)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "附件文件名不能为空");
        }
        String cleaned = StringUtils.cleanPath(original).replace('\\', '/');
        String fileName = Paths.get(cleaned).getFileName().toString();
        if (fileName.contains("..") || fileName.length() > 255) {
            throw new BizException(ErrorCode.BAD_REQUEST, "附件文件名不合法");
        }
        return fileName;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void validateSignature(byte[] content, String extension) {
        if ("docx".equals(extension)) {
            boolean zip = content.length >= 4
                    && content[0] == 0x50 && content[1] == 0x4B
                    && content[2] == 0x03 && content[3] == 0x04;
            if (!zip) {
                throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "docx 文件内容不合法");
            }
        } else if ("pdf".equals(extension)) {
            boolean pdf = content.length >= 5
                    && content[0] == '%' && content[1] == 'P'
                    && content[2] == 'D' && content[3] == 'F' && content[4] == '-';
            if (!pdf) {
                throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "pdf 文件内容不合法");
            }
        }
    }

    private String contentType(String extension) {
        switch (extension) {
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "pdf":
                return "application/pdf";
            case "md":
                return "text/markdown;charset=UTF-8";
            case "txt":
            default:
                return "text/plain;charset=UTF-8";
        }
    }

    @Getter
    public static class DownloadFile {
        private final Resource resource;
        private final String fileName;
        private final String contentType;

        public DownloadFile(Resource resource, String fileName, String contentType) {
            this.resource = resource;
            this.fileName = fileName;
            this.contentType = contentType;
        }
    }
}
