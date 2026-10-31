package com.taiji.fund.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.ProjectDocument;
import com.taiji.fund.system.user.User;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-16 ~ UT-21：附件上传、四种格式解析、章节抽取、限制、重试、下载与正文截断。
 */
class ProjectDocumentApiTest extends AbstractProjectApiTest {

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PDF = "application/pdf";
    private static final String TEXT = "text/plain";

    @Test
    @DisplayName("UT-16 docx 上传后解析出目标/研制内容/关键技术/节点章节")
    void ut16_docxUploadParsesSections() throws Exception {
        User owner = createGroupUser("ut16", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut16-项目"), owner.getGroupId(), owner.getId());

        JsonNode document = upload(token, projectId, "申报书.docx", DOCX, docxWithSections());
        assertThat(document.path("parseStatus").asText()).isEqualTo("PARSED");
        long documentId = document.path("id").asLong();

        JsonNode preview = preview(token, projectId, documentId);
        assertThat(preview.path("parsed").asBoolean()).isTrue();
        assertThat(preview.path("goals").size()).isGreaterThanOrEqualTo(1);
        assertThat(preview.path("contents").size()).isGreaterThanOrEqualTo(1);
        assertThat(preview.path("keyTechs").size()).isGreaterThanOrEqualTo(1);
        assertThat(preview.path("milestones").size()).isGreaterThanOrEqualTo(1);

        List<String> previewItems = new java.util.ArrayList<>();
        for (String field : Arrays.asList("goals", "contents", "keyTechs", "milestones")) {
            preview.path(field).forEach(node -> previewItems.add(node.asText()));
        }
        assertThat(previewItems).isNotEmpty()
                .allSatisfy(item -> assertThat(item)
                        .doesNotContain("项目目标")
                        .doesNotContain("研制内容")
                        .doesNotContain("关键技术")
                        .doesNotContain("节点计划"));

        String text = projectDocumentMapper.selectById(documentId).getTextContent();
        assertThat(text).contains("项目目标").contains("研制内容").contains("关键技术").contains("节点计划")
                .contains("完成多模态缺陷识别算法研究");
    }

    @Test
    @DisplayName("UT-17 PDF/TXT/MD 均可解析，中文正文无乱码")
    void ut17_pdfTxtMdParsed() throws Exception {
        User owner = createGroupUser("ut17", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut17-项目"), owner.getGroupId(), owner.getId());

        JsonNode pdf = upload(token, projectId, "申报书.pdf", PDF, pdfWithChinese());
        assertThat(pdf.path("parseStatus").asText()).isEqualTo("PARSED");
        assertThat(projectDocumentMapper.selectById(pdf.path("id").asLong()).getTextContent())
                .contains("项目目标").contains("完成多模态缺陷识别算法研究");

        byte[] txt = "一、项目目标\n完成多模态缺陷识别算法研究\n二、研制内容\n构建数据标注平台\n"
                .getBytes(StandardCharsets.UTF_8);
        JsonNode txtDoc = upload(token, projectId, "申报书.txt", TEXT, txt);
        assertThat(txtDoc.path("parseStatus").asText()).isEqualTo("PARSED");
        assertThat(projectDocumentMapper.selectById(txtDoc.path("id").asLong()).getTextContent())
                .contains("项目目标").contains("构建数据标注平台");

        byte[] md = ("# 项目目标\n完成多模态缺陷识别算法研究\n\n"
                + "[参考链接](http://example.com)\n\n```java\nint a = 1;\n```\n")
                .getBytes(StandardCharsets.UTF_8);
        JsonNode mdDoc = upload(token, projectId, "申报书.md", "text/markdown", md);
        assertThat(mdDoc.path("parseStatus").asText()).isEqualTo("PARSED");
        String mdText = projectDocumentMapper.selectById(mdDoc.path("id").asLong()).getTextContent();
        assertThat(mdText).contains("项目目标").contains("完成多模态缺陷识别算法研究")
                .contains("参考链接")
                .doesNotContain("](").doesNotContain("```").doesNotContain("int a = 1;");
    }

    @Test
    @DisplayName("UT-18 非法类型、超过 20MB、第 11 个附件均被拒绝且不落库")
    void ut18_invalidTypeSizeAndCountRejected() throws Exception {
        User owner = createGroupUser("ut18", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut18-项目"), owner.getGroupId(), owner.getId());

        JsonNode exeRejected = uploadExpectingBadRequest(token, projectId, "恶意.exe", "application/octet-stream",
                new byte[]{'M', 'Z', 0, 0});
        assertThat(exeRejected.path("code").asInt()).isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode());
        assertThat(documentCount(projectId)).isZero();

        byte[] tooLarge = new byte[20 * 1024 * 1024 + 1];
        JsonNode largeRejected = uploadExpectingBadRequest(token, projectId, "超大.txt", TEXT, tooLarge);
        assertThat(largeRejected.path("code").asInt()).isEqualTo(ErrorCode.FILE_TOO_LARGE.getCode());
        assertThat(documentCount(projectId)).isZero();

        for (int index = 1; index <= 10; index++) {
            JsonNode accepted = upload(token, projectId, "附件" + index + ".txt", TEXT,
                    ("附件正文" + index).getBytes(StandardCharsets.UTF_8));
            assertThat(accepted.path("parseStatus").asText()).isEqualTo("PARSED");
        }
        assertThat(documentCount(projectId)).isEqualTo(10);

        JsonNode limitRejected = uploadExpectingBadRequest(token, projectId, "附件11.txt", TEXT,
                "第十一个附件".getBytes(StandardCharsets.UTF_8));
        assertThat(limitRejected.path("code").asInt()).isEqualTo(ErrorCode.FILE_LIMIT_EXCEEDED.getCode());
        assertThat(documentCount(projectId)).isEqualTo(10);
    }

    @Test
    @DisplayName("UT-19 损坏 docx / 无文本层 PDF 解析失败且有原因，重试不抛 500")
    void ut19_parseFailureAndRetry() throws Exception {
        User owner = createGroupUser("ut19", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut19-项目"), owner.getGroupId(), owner.getId());

        byte[] brokenDocx = new byte[]{0x50, 0x4B, 0x03, 0x04, 1, 2, 3, 4, 5, 6, 7, 8};
        JsonNode broken = upload(token, projectId, "损坏.docx", DOCX, brokenDocx);
        assertThat(broken.path("parseStatus").asText()).isEqualTo("FAILED");
        assertThat(broken.path("parseMessage").asText()).isNotBlank();
        long brokenId = broken.path("id").asLong();
        assertThat(projectDocumentMapper.selectById(brokenId).getTextContent()).isNull();

        MvcResult retry = mockMvc.perform(post("/api/v1/documents/{id}/parse", brokenId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode retried = readJson(retry).path("data");
        assertThat(retried.path("parseStatus").asText()).isEqualTo("FAILED");
        assertThat(retried.path("parseMessage").asText()).isNotBlank();

        JsonNode emptyPdf = upload(token, projectId, "扫描件.pdf", PDF, emptyPdf());
        assertThat(emptyPdf.path("parseStatus").asText()).isEqualTo("FAILED");
        assertThat(emptyPdf.path("parseMessage").asText()).contains("文本层");
    }

    @Test
    @DisplayName("UT-20 合法下载返回文件流与编码文件名，不存在 404，路径穿越被拒")
    void ut20_downloadAndPathSafety() throws Exception {
        User owner = createGroupUser("ut20", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut20-项目"), owner.getGroupId(), owner.getId());

        byte[] content = "下载内容验证".getBytes(StandardCharsets.UTF_8);
        JsonNode document = upload(token, projectId, "申报说明.txt", TEXT, content);
        long documentId = document.path("id").asLong();

        MvcResult download = mockMvc.perform(get("/api/v1/documents/{id}/download", documentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(download.getResponse().getContentType()).startsWith("text/plain");
        assertThat(download.getResponse().getContentAsByteArray()).isEqualTo(content);
        String disposition = download.getResponse().getHeader("Content-Disposition");
        assertThat(disposition).isNotNull().contains("filename*=UTF-8''");

        MvcResult missing = mockMvc.perform(get("/api/v1/documents/{id}/download", 987654321L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andReturn();
        assertThat(readJson(missing).path("code").asInt()).isEqualTo(ErrorCode.FILE_NOT_FOUND.getCode());

        ProjectDocument forged = new ProjectDocument();
        forged.setProjectId(projectId);
        forged.setFileName("越权文件.txt");
        forged.setStoredName("forged.txt");
        forged.setFileType("txt");
        forged.setFileSize(1L);
        forged.setStoragePath("../outside.txt");
        forged.setParseStatus("PARSED");
        forged.setUploadedBy(owner.getId());
        forged.setCreatedAt(LocalDateTime.now());
        projectDocumentMapper.insert(forged);

        MvcResult traversal = mockMvc.perform(get("/api/v1/documents/{id}/download", forged.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(readJson(traversal).path("code").asInt()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(readJson(traversal).path("message").asText()).contains("路径非法");
    }

    @Test
    @DisplayName("UT-21 超过 200,000 字符的正文被截断且解析成功")
    void ut21_longTextTruncated() throws Exception {
        User owner = createGroupUser("ut21", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut21-项目"), owner.getGroupId(), owner.getId());

        char[] chars = new char[250_000];
        Arrays.fill(chars, 'a');
        JsonNode document = upload(token, projectId, "长文本.txt", TEXT,
                new String(chars).getBytes(StandardCharsets.UTF_8));

        assertThat(document.path("parseStatus").asText()).isEqualTo("PARSED");
        String text = projectDocumentMapper.selectById(document.path("id").asLong()).getTextContent();
        assertThat(text).hasSize(200_000);
    }

    private JsonNode upload(String token, long projectId, String fileName, String contentType, byte[] bytes)
            throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/projects/{id}/documents", projectId)
                        .file(new MockMultipartFile("file", fileName, contentType, bytes))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = readJson(result);
        assertThat(body.path("code").asInt()).isZero();
        return body.path("data");
    }

    private JsonNode uploadExpectingBadRequest(String token, long projectId, String fileName,
                                               String contentType, byte[] bytes) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/projects/{id}/documents", projectId)
                        .file(new MockMultipartFile("file", fileName, contentType, bytes))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andReturn();
        return readJson(result);
    }

    private JsonNode preview(String token, long projectId, long documentId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/projects/{projectId}/documents/{documentId}/sections",
                        projectId, documentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private long documentCount(long projectId) {
        return projectDocumentMapper.selectCount(com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<ProjectDocument>lambdaQuery().eq(ProjectDocument::getProjectId, projectId));
    }

    private byte[] docxWithSections() throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            paragraph(document, "一、项目目标");
            paragraph(document, "完成多模态缺陷识别算法研究");
            paragraph(document, "二、研制内容");
            paragraph(document, "构建多模态数据采集与标注平台");
            paragraph(document, "三、关键技术");
            paragraph(document, "多模态特征融合");
            paragraph(document, "四、节点计划");
            paragraph(document, "第一阶段原型开发");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.write(output);
            return output.toByteArray();
        }
    }

    private void paragraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.createRun().setText(text);
    }

    private byte[] pdfWithChinese() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType0Font font = loadChineseFont(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(font, 14);
                stream.newLineAtOffset(50, 760);
                stream.showText("一、项目目标");
                stream.newLineAtOffset(0, -24);
                stream.showText("完成多模态缺陷识别算法研究");
                stream.newLineAtOffset(0, -24);
                stream.showText("二、研制内容");
                stream.newLineAtOffset(0, -24);
                stream.showText("构建数据标注平台");
                stream.endText();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private PDType0Font loadChineseFont(PDDocument document) throws IOException {
        File simhei = new File("C:/Windows/Fonts/simhei.ttf");
        if (simhei.exists()) {
            return PDType0Font.load(document, simhei);
        }
        File simsun = new File("C:/Windows/Fonts/simsun.ttc");
        if (simsun.exists()) {
            TrueTypeCollection collection = new TrueTypeCollection(simsun);
            TrueTypeFont font = collection.getFontByName("SimSun");
            if (font != null) {
                return PDType0Font.load(document, font, true);
            }
        }
        throw new IOException("测试环境缺少可嵌入的中文字体");
    }

    private byte[] emptyPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.A4));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
