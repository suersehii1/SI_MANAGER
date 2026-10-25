package com.sipm.issue;

import com.sipm.attachment.AttachmentRepository;
import com.sipm.attachment.FileStorageService;
import com.sipm.attachment.dto.AttachmentResponse;
import com.sipm.common.error.BusinessException;
import com.sipm.common.error.OptimisticLocks;
import com.sipm.common.page.PageResponse;
import com.sipm.issue.dto.IssueDetail;
import com.sipm.issue.dto.IssueListItem;
import com.sipm.issue.dto.IssueRequest;
import com.sipm.project.Project;
import com.sipm.project.ProjectRepository;
import com.sipm.security.AppUserPrincipal;
import com.sipm.user.UserRepository;
import com.sipm.user.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService storage;

    public IssueService(IssueRepository issueRepository, ProjectRepository projectRepository,
                        UserRepository userRepository, UserService userService,
                        AttachmentRepository attachmentRepository, FileStorageService storage) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.attachmentRepository = attachmentRepository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueListItem> search(Long projectId, IssueStatus status, IssuePriority priority,
                                              Long assigneeId, String keyword, Pageable pageable) {
        Specification<Issue> spec = Specification.where(IssueSpecifications.projectId(projectId))
                .and(IssueSpecifications.status(status))
                .and(IssueSpecifications.priority(priority))
                .and(IssueSpecifications.assigneeId(assigneeId))
                .and(IssueSpecifications.keyword(keyword));
        return PageResponse.of(issueRepository.findAll(spec, pageable), IssueListItem::from);
    }

    @Transactional(readOnly = true)
    public IssueDetail get(Long id) {
        Issue issue = issueRepository.findWithRefsById(id).orElseThrow(() -> BusinessException.notFound("이슈"));
        return toDetail(issue);
    }

    @Transactional
    public IssueDetail create(IssueRequest req, AppUserPrincipal me) {
        Issue issue = new Issue();
        apply(issue, req);
        issue.setReporter(userRepository.getReferenceById(me.getId()));
        issue = issueRepository.saveAndFlush(issue);
        return get(issue.getId());
    }

    @Transactional
    public IssueDetail update(Long id, IssueRequest req) {
        Issue issue = issueRepository.findWithRefsById(id).orElseThrow(() -> BusinessException.notFound("이슈"));
        OptimisticLocks.check(issue.getVersion(), req.version()); // 다른 사람이 먼저 수정했으면 409
        apply(issue, req);
        issueRepository.saveAndFlush(issue);
        return toDetail(issue);
    }

    @Transactional
    public void delete(Long id, AppUserPrincipal me) {
        Issue issue = issueRepository.findWithRefsById(id).orElseThrow(() -> BusinessException.notFound("이슈"));
        if (!me.isAdmin() && !issue.getReporter().getId().equals(me.getId())) {
            throw new AccessDeniedException("Not the reporter");
        }
        issueRepository.delete(issue);
        issueRepository.flush();
        storage.deleteIssueDirsAfterCommit(List.of(id));
    }

    /** 요청 → 엔티티 반영 (생성/수정 공통, 전체 교체). 담당자는 존재(404)·활성(400 USER_DISABLED)만 검사. */
    private void apply(Issue issue, IssueRequest req) {
        Project project = projectRepository.findById(req.projectId())
                .orElseThrow(() -> BusinessException.notFound("프로젝트"));
        issue.setProject(project);
        issue.setTitle(req.title().trim());
        issue.setDescription(req.description());
        issue.setStatus(req.status() != null ? req.status() : IssueStatus.OPEN);
        issue.setPriority(req.priority() != null ? req.priority() : IssuePriority.MEDIUM);
        issue.setProgress(req.progress() != null ? req.progress() : 0);
        issue.setAssignee(req.assigneeId() != null ? userService.getEnabled(req.assigneeId()) : null);
        issue.setDueDate(req.dueDate());
    }

    private IssueDetail toDetail(Issue issue) {
        List<AttachmentResponse> attachments = attachmentRepository.findByIssueIdFetch(issue.getId())
                .stream().map(AttachmentResponse::from).toList();
        return IssueDetail.from(issue, attachments);
    }
}
