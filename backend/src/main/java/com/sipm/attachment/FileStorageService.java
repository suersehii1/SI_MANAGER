package com.sipm.attachment;

import com.sipm.common.error.BusinessException;
import com.sipm.common.error.ErrorCode;
import com.sipm.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 첨부파일 검증 + 격리 저장 (specs §7).
 * - 2MB 3중 방어 중 서비스 계층(④): Spring multipart 한도(③, 413)를 우회한 요청(MockMvc 등)도 여기서 400으로 거부.
 * - 저장 경로는 {root}/issues/{issueId}/{UUID}.{ext}. 사용자가 보낸 원본 파일명은 경로에 절대 쓰지 않는다
 *   (DB original_filename / Content-Disposition에만 사용) → 경로 조작(../) 원천 차단.
 * - 경로 해석 후 반드시 root 하위인지 startsWith로 재확인한다.
 */
@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    public static final long MAX_FILE_SIZE = 2L * 1024 * 1024; // 2,097,152 bytes (inclusive)
    public static final Pattern STORED_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z0-9]{1,10}$");

    public static final Map<String, String> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("txt", "text/plain"),
            Map.entry("log", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("md", "text/markdown"),
            Map.entry("zip", "application/zip"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("hwp", "application/x-hwp"));

    private final Path rootReal;

    public FileStorageService(StorageProperties props) {
        try {
            Path root = Paths.get(props.root()).toAbsolutePath();
            Files.createDirectories(root);
            this.rootReal = root.toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot initialize storage root " + props.root(), e);
        }
    }

    public Path getRoot() {
        return rootReal;
    }

    /** Validated upload metadata. */
    public record ValidatedFile(String originalFilename, String extension, String contentType, long size) {
    }

    /** Validation order: empty -> size -> filename -> extension. */
    public ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        String name = sanitize(file.getOriginalFilename());
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        String contentType = ALLOWED_TYPES.get(ext);
        if (contentType == null) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        return new ValidatedFile(name, ext, contentType, file.getSize());
    }

    /** Sanitizes the client file name (section 7.3). Throws INVALID_FILENAME when nothing usable remains. */
    public static String sanitize(String original) {
        if (original == null) {
            return "file";
        }
        String s = Normalizer.normalize(original, Normalizer.Form.NFC).replace('\\', '/');
        int slash = s.lastIndexOf('/');
        if (slash >= 0) {
            s = s.substring(slash + 1);
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c <= 0x1F || c == 0x7F || "<>:\"|?*".indexOf(c) >= 0) {
                continue;
            }
            sb.append(c);
        }
        s = trimSpacesAndDots(sb.toString());
        if (s.isEmpty() || s.equals(".") || s.equals("..")) {
            throw new BusinessException(ErrorCode.INVALID_FILENAME);
        }
        if (s.length() > 255) {
            int dot = s.lastIndexOf('.');
            String ext = dot > 0 && s.length() - dot <= 11 ? s.substring(dot) : "";
            s = s.substring(0, 255 - ext.length()) + ext;
        }
        return s;
    }

    private static String trimSpacesAndDots(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && (Character.isWhitespace(s.charAt(start)) || s.charAt(start) == '.')) {
            start++;
        }
        while (end > start && (Character.isWhitespace(s.charAt(end - 1)) || s.charAt(end - 1) == '.')) {
            end--;
        }
        return s.substring(start, end);
    }

    public String newStoredFilename(String ext) {
        return UUID.randomUUID() + "." + ext;
    }

    /** Resolves {root}/issues/{issueId}/{storedFilename} and verifies it stays under root. */
    public Path resolve(long issueId, String storedFilename) {
        if (storedFilename == null || !STORED_NAME.matcher(storedFilename).matches()) {
            log.error("Invalid stored filename: {}", storedFilename);
            throw new IllegalStateException("Invalid stored filename");
        }
        Path target = rootReal.resolve("issues").resolve(Long.toString(issueId)).resolve(storedFilename).normalize();
        if (!target.startsWith(rootReal)) {
            log.error("Path escapes storage root: {}", target);
            throw new IllegalStateException("Path escapes storage root");
        }
        return target;
    }

    public Path issueDir(long issueId) {
        Path dir = rootReal.resolve("issues").resolve(Long.toString(issueId)).normalize();
        if (!dir.startsWith(rootReal)) {
            throw new IllegalStateException("Path escapes storage root");
        }
        return dir;
    }

    /** Writes the stream to the target (fails if the file already exists). */
    public void write(Path target, MultipartFile file) {
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }
    }

    public void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", path, e.getMessage());
        }
    }

    public void deleteIssueDirQuietly(long issueId) {
        Path dir = issueDir(issueId);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) {
                deleteQuietly(p);
            }
        } catch (IOException e) {
            log.warn("Failed to delete issue directory {}: {}", dir, e.getMessage());
        }
    }

    /** Runs after the current transaction commits (or immediately when no transaction is active). */
    public void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /**
     * 파일은 DB 커밋 전에 디스크에 먼저 쓰인다. 트랜잭션이 롤백되면(커밋 실패 포함) 방금 쓴 파일을 지워
     * DB 행 없이 디스크에만 남는 고아 파일을 막는다.
     */
    public void deleteOnRollback(Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteQuietly(path);
                }
            }
        });
    }

    public void deleteIssueDirsAfterCommit(Collection<Long> issueIds) {
        List<Long> ids = List.copyOf(issueIds);
        if (ids.isEmpty()) {
            return;
        }
        afterCommit(() -> ids.forEach(this::deleteIssueDirQuietly));
    }
}
