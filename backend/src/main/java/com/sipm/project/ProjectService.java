package com.sipm.project;

import com.sipm.assignment.AssignmentRepository;
import com.sipm.assignment.ProjectAssignment;
import com.sipm.attachment.FileStorageService;
import com.sipm.common.error.BusinessException;
import com.sipm.common.error.OptimisticLocks;
import com.sipm.common.error.ErrorCode;
import com.sipm.common.page.PageResponse;
import com.sipm.common.util.LikePatterns;
import com.sipm.issue.IssueRepository;
import com.sipm.project.dto.ProjectRequest;
import com.sipm.project.dto.ProjectResponse;
import com.sipm.project.dto.ProjectSummary;
import com.sipm.security.AppUserPrincipal;
import com.sipm.user.UserRepository;
import com.sipm.wbs.WbsItem;
import com.sipm.wbs.WbsItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 프로젝트 CRUD. 계획 기간 변경 시 기존 할당/WBS 리프가 범위를 벗어나면 409 PROJECT_PERIOD_CONFLICT. */
@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final AssignmentRepository assignmentRepository;
    private final IssueRepository issueRepository;
    private final WbsItemRepository wbsItemRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;

    public ProjectService(ProjectRepository projectRepository, AssignmentRepository assignmentRepository,
                          IssueRepository issueRepository, WbsItemRepository wbsItemRepository,
                          UserRepository userRepository, FileStorageService storage) {
        this.projectRepository = projectRepository;
        this.assignmentRepository = assignmentRepository;
        this.issueRepository = issueRepository;
        this.wbsItemRepository = wbsItemRepository;
        this.userRepository = userRepository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Project get(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> BusinessException.notFound("프로젝트"));
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> lookup() {
        List<ProjectSummary> result = new ArrayList<>();
        for (Project p : projectRepository.findAll(Sort.by("code").ascending())) {
            result.add(ProjectSummary.from(p));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> search(String keyword, ProjectStatus status, Pageable pageable) {
        Specification<Project> spec = Specification.where(ProjectRepository.keyword(LikePatterns.contains(keyword)))
                .and(ProjectRepository.status(status));
        Page<Project> page = projectRepository.findAll(spec, pageable);

        // 페이지 내 프로젝트들의 할당 인원 수를 한 번의 group by 쿼리로 조회 (N+1 방지)
        Map<Long, Long> counts = new HashMap<>();
        List<Long> ids = page.getContent().stream().map(Project::getId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : assignmentRepository.countByProjectIds(ids)) {
                counts.put((Long) row[0], (Long) row[1]);
            }
        }
        return PageResponse.of(page, p -> ProjectResponse.from(p, counts.getOrDefault(p.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getResponse(Long id) {
        Project p = get(id);
        return ProjectResponse.from(p, assignmentRepository.countByProjectId(id));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest req, AppUserPrincipal me) {
        if (projectRepository.existsByCode(req.getCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PROJECT_CODE);
        }
        Project p = new Project();
        apply(p, req);
        p.setCreatedBy(userRepository.getReferenceById(me.getId()));
        p = projectRepository.saveAndFlush(p);
        return ProjectResponse.from(p, 0);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest req) {
        Project p = get(id);
        OptimisticLocks.check(p.getVersion(), req.getVersion()); // 다른 사람이 먼저 수정했으면 409
        if (projectRepository.existsByCodeAndIdNot(req.getCode(), id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PROJECT_CODE);
        }
        checkPeriodConflicts(id, req.getPlannedStartDate(), req.getPlannedEndDate());
        apply(p, req);
        projectRepository.saveAndFlush(p);
        return ProjectResponse.from(p, assignmentRepository.countByProjectId(id));
    }

    @Transactional
    public void delete(Long id) {
        Project p = get(id);
        // 첨부 디렉터리 정리를 위해 삭제 전에 이슈 id 목록을 확보 (DB 행은 CASCADE로 삭제됨)
        List<Long> issueIds = issueRepository.findIdsByProjectId(id);
        projectRepository.delete(p);
        projectRepository.flush();
        storage.deleteIssueDirsAfterCommit(issueIds);
    }

    /**
     * 새 계획 기간이 기존 할당 계획 기간과 WBS 리프 계획 기간을 모두 포함하는지 검사한다.
     * 위반 시 409 PROJECT_PERIOD_CONFLICT, 메시지에 위반 대상(사용자 이름 / WBS 작업명)을 나열.
     */
    private void checkPeriodConflicts(Long projectId, LocalDate newStart, LocalDate newEnd) {
        List<String> violations = new ArrayList<>();
        for (ProjectAssignment a : assignmentRepository.findByProjectIdFetch(projectId)) {
            if (a.getPlannedStart().isBefore(newStart) || a.getPlannedEnd().isAfter(newEnd)) {
                violations.add(a.getUser().getName());
            }
        }

        // WBS: 자식이 없는 노드(리프)의 저장된 계획 기간만 검사 (부모는 집계값이므로 제외)
        List<WbsItem> wbs = wbsItemRepository.findTreeByProjectId(projectId);
        Set<Long> parentIds = new HashSet<>();
        for (WbsItem w : wbs) {
            if (w.getParentId() != null) {
                parentIds.add(w.getParentId());
            }
        }
        for (WbsItem w : wbs) {
            if (parentIds.contains(w.getId())) {
                continue;
            }
            boolean startOut = w.getPlannedStart() != null && w.getPlannedStart().isBefore(newStart);
            boolean endOut = w.getPlannedEnd() != null && w.getPlannedEnd().isAfter(newEnd);
            if (startOut || endOut) {
                violations.add("WBS '" + w.getName() + "'");
            }
        }

        if (!violations.isEmpty()) {
            throw new BusinessException(ErrorCode.PROJECT_PERIOD_CONFLICT,
                    "기존 할당/WBS가 새 프로젝트 계획 기간을 벗어납니다: " + String.join(", ", violations));
        }
    }

    private void apply(Project p, ProjectRequest req) {
        p.setCode(req.getCode());
        p.setName(req.getName().trim());
        p.setClientName(blankToNull(req.getClientName()));
        p.setDescription(blankToNull(req.getDescription()));
        p.setStatus(req.getStatus() != null ? req.getStatus() : ProjectStatus.PLANNED);
        p.setPlannedStartDate(req.getPlannedStartDate());
        p.setPlannedEndDate(req.getPlannedEndDate());
        p.setActualStartDate(req.getActualStartDate());
        p.setActualEndDate(req.getActualEndDate());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
