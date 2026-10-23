package com.sipm.attachment;

import com.sipm.attachment.dto.AttachmentResponse;
import com.sipm.common.error.BusinessException;
import com.sipm.issue.Issue;
import com.sipm.issue.IssueRepository;
import com.sipm.security.AppUserPrincipal;
import com.sipm.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class AttachmentService {
    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    private final AttachmentRepository attachmentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;

    public AttachmentService(AttachmentRepository attachmentRepository, IssueRepository issueRepository,
                             UserRepository userRepository, FileStorageService storage) {
        this.attachmentRepository = attachmentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.storage = storage;
    }

    /** Download payload. */
    public record DownloadFile(Path path, String originalFilename, String contentType, long size) {
    }

    @Transactional
    public AttachmentResponse upload(Long issueId, MultipartFile file, AppUserPrincipal me) {
        Issue issue = issueRepository.findById(issueId).orElseThrow(() -> BusinessException.notFound("이슈"));
        FileStorageService.ValidatedFile vf = storage.validate(file);
        String storedName = storage.newStoredFilename(vf.extension());
        Path target = storage.resolve(issue.getId(), storedName);
        // 보상 처리: 쓰기 도중 실패하거나 이후 DB 저장/커밋이 실패해 롤백되면 디스크 파일도 삭제.
        // 쓰기 전에 등록해 두어야 부분적으로 쓰인 파일도 정리된다(없는 파일 삭제는 무시됨).
        storage.deleteOnRollback(target);
        storage.write(target, file);

        IssueAttachment a = new IssueAttachment();
        a.setIssue(issue);
        a.setOriginalFilename(vf.originalFilename());
        a.setStoredFilename(storedName);
        a.setContentType(vf.contentType());
        a.setSizeBytes(vf.size());
        a.setUploadedBy(userRepository.getReferenceById(me.getId()));
        a = attachmentRepository.saveAndFlush(a);
        IssueAttachment saved = attachmentRepository.findByIdFetch(a.getId()).orElseThrow();
        return AttachmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw BusinessException.notFound("이슈");
        }
        return attachmentRepository.findByIssueIdFetch(issueId).stream().map(AttachmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DownloadFile download(Long id) {
        IssueAttachment a = attachmentRepository.findByIdFetch(id)
                .orElseThrow(() -> BusinessException.notFound("첨부파일"));
        Path path = storage.resolve(a.getIssue().getId(), a.getStoredFilename());
        if (!Files.exists(path)) {
            log.warn("Attachment file missing on disk: id={}, path={}", id, path);
            throw BusinessException.notFound("첨부파일");
        }
        return new DownloadFile(path, a.getOriginalFilename(), a.getContentType(), a.getSizeBytes());
    }

    /** 업로더 본인 또는 ADMIN만 삭제 가능(그 외 403). DB 행을 지우고, 디스크 파일은 커밋이 성공한 뒤에 지운다. */
    @Transactional
    public void delete(Long id, AppUserPrincipal me) {
        IssueAttachment a = attachmentRepository.findByIdFetch(id)
                .orElseThrow(() -> BusinessException.notFound("첨부파일"));
        if (!me.isAdmin() && !a.getUploadedBy().getId().equals(me.getId())) {
            throw new AccessDeniedException("Not the uploader");
        }
        Path path = storage.resolve(a.getIssue().getId(), a.getStoredFilename());
        attachmentRepository.delete(a);
        storage.afterCommit(() -> storage.deleteQuietly(path));
    }
}
