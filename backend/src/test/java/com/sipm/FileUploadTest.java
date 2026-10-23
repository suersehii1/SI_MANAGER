package com.sipm;

import com.fasterxml.jackson.databind.JsonNode;
import com.sipm.attachment.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA 체크리스트 2(파일 업로드 2MB 제한) + 저장 격리/다운로드 검증.
 * - MockMvc는 서블릿 컨테이너 멀티파트 파서를 거치지 않으므로 2MB+1 → 서비스 계층 400 FILE_TOO_LARGE (specs §7.1 ④).
 * - 컨테이너 한도(413)는 RANDOM_PORT로 띄운 실제 Tomcat에 TestRestTemplate으로 보내 검증한다 (specs §7.1 ③).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FileUploadTest extends ApiTestSupport {
    private static final int TWO_MB = 2 * 1024 * 1024; // 2,097,152

    @Autowired
    TestRestTemplate rest;
    @Autowired
    FileStorageService storage;

    // ===================== ≤ 2MB 성공 =====================

    @Test
    void oneKilobyteUploadSucceedsAndDownloadsSameBytes() throws Exception {
        String admin = admin();
        long issueId = seedIssueId(admin);
        byte[] bytes = pattern(1024);

        // 클라이언트 Content-Type(image/png)은 무시하고 확장자로 서버가 결정한다
        JsonNode created = json(upload(admin, issueId, new MockMultipartFile("file", "보고서.txt", "image/png", bytes))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issueId").value(issueId))
                .andExpect(jsonPath("$.originalFilename").value("보고서.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.sizeBytes").value(1024))
                .andExpect(jsonPath("$.uploadedBy.username").value("admin")));
        long attachmentId = created.get("id").asLong();

        byte[] downloaded = mvc.perform(getAs(admin, "/api/attachments/" + attachmentId + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/plain")))
                .andExpect(header().string("Content-Length", "1024"))
                // O-01: filename은 ASCII 대체(한글 → '_'), filename*는 UTF-8 퍼센트 인코딩 (RFC 2047 형식 금지)
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"___.txt\"; filename*=UTF-8''%EB%B3%B4%EA%B3%A0%EC%84%9C.txt"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).isEqualTo(bytes);

        mvc.perform(getAs(admin, "/api/issues/" + issueId + "/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + attachmentId + ")]").exists());
        mvc.perform(getAs(admin, "/api/issues/" + issueId))
                .andExpect(jsonPath("$.attachmentCount").value(1));
    }

    @Test
    void exactlyTwoMegabytesSucceedsAndIsStoredUnderIsolatedRoot() throws Exception {
        String user = user(); // 일반 사용자도 업로드 가능
        long issueId = seedIssueId(user);
        byte[] bytes = pattern(TWO_MB);

        Path dir = storage.getRoot().resolve("issues").resolve(String.valueOf(issueId));
        List<Path> before = listFiles(dir);
        JsonNode created = json(upload(user, issueId, new MockMultipartFile("file", "spec.pdf", "application/pdf", bytes))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sizeBytes").value(TWO_MB))
                .andExpect(jsonPath("$.contentType").value("application/pdf")));

        // 저장 위치: {root}/issues/{issueId}/{UUID}.pdf — 원본 파일명은 경로에 쓰지 않는다
        List<Path> added = listFiles(dir);
        added.removeAll(before);
        assertThat(added).hasSize(1);
        Path stored = added.get(0);
        assertThat(Files.size(stored)).isEqualTo(TWO_MB);
        assertThat(stored.getFileName().toString()).matches(FileStorageService.STORED_NAME.pattern());
        assertThat(stored.toRealPath().startsWith(storage.getRoot())).isTrue();

        byte[] downloaded = mvc.perform(getAs(user, "/api/attachments/" + created.get("id").asLong() + "/download"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).isEqualTo(bytes);
    }

    // ===================== > 2MB 차단 =====================

    @Test
    void twoMegabytesPlusOneIsRejectedByServiceLayer() throws Exception {
        String admin = admin();
        upload(admin, seedIssueId(admin), new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[TWO_MB + 1]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void twoMegabytesPlusOneIsRejectedWith413ByRealTomcat() throws Exception {
        String admin = admin(); // 로그인은 MockMvc로 (읽기 전용)
        long issueId = seedIssueId(admin);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(new byte[TWO_MB + 1]) {
            @Override
            public String getFilename() {
                return "big.pdf";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", admin);

        ResponseEntity<String> res = rest.exchange("/api/issues/" + issueId + "/attachments", HttpMethod.POST,
                new HttpEntity<>(form, headers), String.class);

        assertThat(res.getStatusCode().value()).isEqualTo(413);
        JsonNode body = om.readTree(res.getBody());
        assertThat(body.get("code").asText()).isEqualTo("FILE_TOO_LARGE");
        assertThat(body.get("path").asText()).isEqualTo("/api/issues/" + issueId + "/attachments");
    }

    // ===================== 기타 검증 =====================

    @Test
    void emptyOrMissingFileIsRejected() throws Exception {
        String admin = admin();
        long issueId = seedIssueId(admin);
        upload(admin, issueId, new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_EMPTY"));
        mvc.perform(multipart("/api/issues/" + issueId + "/attachments").header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_EMPTY"));
    }

    @Test
    void disallowedExtensionsAreRejected() throws Exception {
        String admin = admin();
        long issueId = seedIssueId(admin);
        for (String name : new String[]{"run.exe", "page.html", "x.svg", "script.sh", "README"}) {
            upload(admin, issueId, new MockMultipartFile("file", name, "application/octet-stream", pattern(10)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("FILE_TYPE_NOT_ALLOWED"));
        }
    }

    @Test
    void invalidFilenameIsRejected() throws Exception {
        String admin = admin();
        upload(admin, seedIssueId(admin), new MockMultipartFile("file", "..", "text/plain", pattern(10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILENAME"));
    }

    @Test
    void pathTraversalFilenameIsSanitizedAndStaysInsideRoot() throws Exception {
        String admin = admin();
        long issueId = seedIssueId(admin);
        upload(admin, issueId, new MockMultipartFile("file", "../../evil.txt", "text/plain", pattern(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("evil.txt"));
        upload(admin, issueId, new MockMultipartFile("file", "C:\\temp\\..\\보고서.pdf", "application/pdf", pattern(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("보고서.pdf"));

        // 루트 밖(상위 디렉터리)이나 루트 안 어디에도 원본 이름의 파일이 생기지 않아야 한다
        Path root = storage.getRoot();
        assertThat(Files.exists(root.getParent().resolve("evil.txt"))).isFalse();
        assertThat(Files.exists(root.getParent().getParent().resolve("evil.txt"))).isFalse();
        try (Stream<Path> walk = Files.walk(root)) {
            assertThat(walk.map(p -> p.getFileName().toString())).doesNotContain("evil.txt", "보고서.pdf");
        }
    }

    @Test
    void uploadToMissingIssueIsNotFound() throws Exception {
        upload(admin(), 999999L, new MockMultipartFile("file", "a.txt", "text/plain", pattern(10)))
                .andExpect(status().isNotFound());
    }

    @Test
    void attachmentDeleteIsAllowedOnlyForUploaderOrAdmin() throws Exception {
        String admin = admin();
        String user = user();
        long issueId = seedIssueId(admin);
        long byAdmin = json(upload(admin, issueId, new MockMultipartFile("file", "a.txt", "text/plain", pattern(10))))
                .get("id").asLong();
        long byUser = json(upload(user, issueId, new MockMultipartFile("file", "b.txt", "text/plain", pattern(10))))
                .get("id").asLong();

        mvc.perform(deleteAs(user, "/api/attachments/" + byAdmin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(deleteAs(user, "/api/attachments/" + byUser)).andExpect(status().isNoContent());
        mvc.perform(deleteAs(admin, "/api/attachments/" + byAdmin)).andExpect(status().isNoContent());
        mvc.perform(getAs(admin, "/api/attachments/" + byAdmin + "/download")).andExpect(status().isNotFound());
    }

    // ===================== 헬퍼 =====================

    private ResultActions upload(String token, long issueId, MockMultipartFile file) throws Exception {
        return mvc.perform(multipart("/api/issues/" + issueId + "/attachments").file(file)
                .header("Authorization", token));
    }

    private long seedIssueId(String token) throws Exception {
        return json(mvc.perform(getAs(token, "/api/issues").param("keyword", "로그인"))).get("content").get(0).get("id").asLong();
    }

    /** 0이 아닌 반복 패턴 바이트 (다운로드 바이트 비교가 의미 있도록). */
    private static byte[] pattern(int size) {
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            b[i] = (byte) (i % 251);
        }
        return b;
    }

    private static List<Path> listFiles(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return new ArrayList<>(files.toList());
        }
    }
}
