package com.taiji.fund.project.service;

import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final Path root;

    public DocumentStorageService(@Value("${app.upload.dir:./storage}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public StoredFile store(byte[] content, String extension) {
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relativeDir = YearMonth.now().format(MONTH_FORMAT);
        Path directory = root.resolve(relativeDir).normalize();
        ensureInsideRoot(directory);
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(storedName).normalize();
            ensureInsideRoot(target);
            Files.write(target, content);
            return new StoredFile(relativeDir + "/" + storedName, storedName);
        } catch (IOException exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "附件保存失败");
        }
    }

    public Resource load(String relativePath) {
        Path target = resolveSafe(relativePath);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        return new FileSystemResource(target);
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(relativePath));
        } catch (IOException ignored) {
            // 删除失败不影响元数据删除，保留可人工清理的文件
        }
    }

    public byte[] readAll(String relativePath) {
        try {
            return Files.readAllBytes(resolveSafe(relativePath));
        } catch (IOException exception) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    public Path root() {
        return root;
    }

    private Path resolveSafe(String relativePath) {
        Path target = root.resolve(relativePath).normalize();
        ensureInsideRoot(target);
        return target;
    }

    private void ensureInsideRoot(Path path) {
        if (!path.startsWith(root)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "附件路径非法");
        }
    }

    @Getter
    public static class StoredFile {
        private final String relativePath;
        private final String storedName;

        public StoredFile(String relativePath, String storedName) {
            this.relativePath = relativePath;
            this.storedName = storedName;
        }
    }
}
