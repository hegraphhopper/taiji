package com.taiji.fund.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class DocumentTextExtractor {

    private static final int MAX_TEXT_LENGTH = 200_000;

    public String extract(byte[] content, String extension) {
        String raw;
        switch (extension) {
            case "docx":
                raw = extractDocx(content);
                break;
            case "pdf":
                raw = extractPdf(content);
                break;
            case "md":
                raw = cleanMarkdown(new String(content, StandardCharsets.UTF_8));
                break;
            case "txt":
                raw = new String(content, StandardCharsets.UTF_8);
                break;
            default:
                throw new DocumentParseException("不支持的文件类型: " + extension);
        }
        return clean(raw);
    }

    private String extractDocx(byte[] content) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(content);
             XWPFDocument document = new XWPFDocument(input)) {
            StringBuilder builder = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    builder.append(text.trim()).append('\n');
                }
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    StringBuilder rowText = new StringBuilder();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            if (rowText.length() > 0) {
                                rowText.append(" | ");
                            }
                            rowText.append(text.trim());
                        }
                    }
                    if (rowText.length() > 0) {
                        builder.append(rowText).append('\n');
                    }
                }
            }
            return builder.toString();
        } catch (IOException | RuntimeException exception) {
            throw new DocumentParseException("docx 解析失败，请确认文件未损坏", exception);
        }
    }

    private String extractPdf(byte[] content) {
        try (PDDocument document = PDDocument.load(content)) {
            if (document.getNumberOfPages() == 0) {
                throw new DocumentParseException("pdf 没有可解析页面");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.trim().isEmpty()) {
                throw new DocumentParseException("pdf 无可提取文本层，扫描件请人工录入");
            }
            return text;
        } catch (DocumentParseException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new DocumentParseException("pdf 解析失败，请确认文件未加密且含文本层", exception);
        }
    }

    private String cleanMarkdown(String text) {
        return text
                .replaceAll("(?s)```.*?```", "\n")
                .replaceAll("!\\[[^\\]]*\\]\\([^\\)]*\\)", "")
                .replaceAll("\\[([^\\]]+)\\]\\([^\\)]*\\)", "$1")
                .replaceAll("(?m)^#{1,6}\\s*", "");
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("(?m)[ ]+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (cleaned.length() > MAX_TEXT_LENGTH) {
            cleaned = cleaned.substring(0, MAX_TEXT_LENGTH);
        }
        return cleaned;
    }
}
