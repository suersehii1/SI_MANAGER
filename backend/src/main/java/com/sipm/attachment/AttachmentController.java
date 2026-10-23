package com.sipm.attachment;

import com.sipm.attachment.dto.AttachmentResponse;
import com.sipm.security.AppUserPrincipal;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/api/issues/{issueId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(@PathVariable Long issueId,
                                                     @RequestParam(value = "file", required = false) MultipartFile file,
                                                     @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(issueId, file, me));
    }

    @GetMapping("/api/issues/{issueId}/attachments")
    public List<AttachmentResponse> list(@PathVariable Long issueId) {
        return attachmentService.list(issueId);
    }

    @GetMapping("/api/attachments/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        AttachmentService.DownloadFile f = attachmentService.download(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(f.contentType()));
        headers.setContentLength(f.size());
        headers.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(f.originalFilename()));
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setCacheControl("private, no-store");
        return ResponseEntity.ok().headers(headers).body(new FileSystemResource(f.path()));
    }

    /**
     * Content-Disposition 헤더를 직접 만든다 (specs §4.13).
     *   attachment; filename="ASCII 대체 이름"; filename*=UTF-8''퍼센트 인코딩 원본 이름
     * - filename: 출력 가능한 ASCII만 남기고 나머지(한글 등)와 따옴표/역슬래시는 '_'로 치환 → 구형 클라이언트용
     * - filename*: RFC 5987/6266 형식. 최신 브라우저는 이 값을 우선 사용
     * (Spring ContentDisposition.filename(name, UTF_8)은 filename에 RFC 2047 "=?UTF-8?Q?...?=" 를 넣어 사용하지 않는다)
     */
    static String contentDisposition(String filename) {
        StringBuilder ascii = new StringBuilder();
        for (char c : filename.toCharArray()) {
            boolean printableAscii = c >= 0x20 && c < 0x7F;
            ascii.append(printableAscii && c != '"' && c != '\\' ? c : '_');
        }
        StringBuilder encoded = new StringBuilder();
        for (byte b : filename.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if (isAttrChar(c)) {
                encoded.append(c);
            } else {
                encoded.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    /** RFC 5987 attr-char: 영문/숫자와 !#$&+-.^_`|~ 는 인코딩하지 않는다. */
    private static boolean isAttrChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || "!#$&+-.^_`|~".indexOf(c) >= 0;
    }

    @DeleteMapping("/api/attachments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        attachmentService.delete(id, me);
        return ResponseEntity.noContent().build();
    }
}
