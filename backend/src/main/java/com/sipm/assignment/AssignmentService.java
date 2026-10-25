package com.sipm.assignment;

import com.sipm.assignment.dto.AssignmentRequest;
import com.sipm.assignment.dto.AssignmentResponse;
import com.sipm.common.error.BusinessException;
import com.sipm.common.error.OptimisticLocks;
import com.sipm.common.error.ErrorCode;
import com.sipm.common.error.FieldErrorItem;
import com.sipm.common.util.Times;
import com.sipm.project.Project;
import com.sipm.project.ProjectService;
import com.sipm.user.User;
import com.sipm.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 프로젝트 인원 할당(투입/철수일 관리). 규칙(specs §5.2):
 * 할당 계획 기간 ⊆ 프로젝트 계획 기간, 같은 사람의 계획 기간 겹침 할당 금지(겹치지 않으면 재할당 허용),
 * 비활성 사용자 할당 금지.
 * 실제 기간(actual)은 프로젝트 기간 제약 없음(지연/조기 투입 허용).
 */
@Service
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final ProjectService projectService;
    private final UserService userService;

    public AssignmentService(AssignmentRepository assignmentRepository, ProjectService projectService,
                             UserService userService) {
        this.assignmentRepository = assignmentRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(Long projectId) {
        projectService.get(projectId); // 404 확인
        LocalDate today = Times.today();
        List<AssignmentResponse> result = new ArrayList<>();
        for (ProjectAssignment a : assignmentRepository.findByProjectIdFetch(projectId)) {
            result.add(AssignmentResponse.from(a, today));
        }
        return result;
    }

    @Transactional
    public AssignmentResponse create(Long projectId, AssignmentRequest req) {
        Project project = projectService.get(projectId);
        User user = userService.getEnabled(req.getUserId());
        checkWithinProjectPeriod(project, req);
        checkNoOverlap(projectId, user, req, null);
        ProjectAssignment a = new ProjectAssignment();
        a.setProject(project);
        a.setUser(user);
        apply(a, req);
        a = assignmentRepository.saveAndFlush(a);
        return AssignmentResponse.from(a, Times.today());
    }

    @Transactional
    public AssignmentResponse update(Long projectId, Long assignmentId, AssignmentRequest req) {
        Project project = projectService.get(projectId);
        ProjectAssignment a = findInProject(projectId, assignmentId);
        OptimisticLocks.check(a.getVersion(), req.getVersion()); // 다른 사람이 먼저 수정했으면 409
        // userId는 변경 불가
        if (!a.getUser().getId().equals(req.getUserId())) {
            throw BusinessException.validation("userId", "IMMUTABLE", "할당된 사용자는 변경할 수 없습니다.", req.getUserId());
        }
        checkWithinProjectPeriod(project, req);
        // 기간을 바꿔서 같은 사람의 다른 할당과 겹치게 되는 것도 막는다 (자기 자신은 제외)
        checkNoOverlap(projectId, a.getUser(), req, a.getId());
        apply(a, req);
        assignmentRepository.saveAndFlush(a);
        return AssignmentResponse.from(a, Times.today());
    }

    @Transactional
    public void delete(Long projectId, Long assignmentId, Long version) {
        projectService.get(projectId);
        ProjectAssignment a = findInProject(projectId, assignmentId);
        OptimisticLocks.check(a.getVersion(), version); // 다른 사람이 먼저 수정했으면 삭제하지 않고 409
        assignmentRepository.delete(a);
    }

    /** 할당이 해당 프로젝트 소속이 아니면 404. */
    private ProjectAssignment findInProject(Long projectId, Long assignmentId) {
        ProjectAssignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> BusinessException.notFound("할당"));
        if (!a.getProject().getId().equals(projectId)) {
            throw BusinessException.notFound("할당");
        }
        return a;
    }

    /**
     * 같은 프로젝트에 같은 사람이 계획 투입 기간이 겹치게 할당되는 것을 막는다 (409 DUPLICATE_ASSIGNMENT).
     * - 같은 사람: 같은 사용자 ID, 또는 이메일이 같은 다른 계정 (이름은 동명이인이 있으므로 보지 않음)
     * - 겹침: 기존 계획 시작 ≤ 새 계획 종료 이고 기존 계획 종료 ≥ 새 계획 시작 (경계일이 같아도 겹침)
     * - 기간이 겹치지 않으면 같은 사람을 다시 할당할 수 있다 (예: 1~3월 투입, 철수 후 6~8월 재투입)
     */
    private void checkNoOverlap(Long projectId, User user, AssignmentRequest req, Long excludeId) {
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        List<ProjectAssignment> overlaps = assignmentRepository.findOverlapping(projectId, user.getId(), email,
                req.getPlannedStart(), req.getPlannedEnd(), excludeId == null ? -1L : excludeId);
        if (!overlaps.isEmpty()) {
            ProjectAssignment o = overlaps.get(0);
            throw new BusinessException(ErrorCode.DUPLICATE_ASSIGNMENT,
                    "같은 사람(" + o.getUser().getName() + ", " + o.getUser().getUsername() + ")이 이미 이 프로젝트에 "
                            + o.getPlannedStart() + " ~ " + o.getPlannedEnd()
                            + " 기간으로 할당되어 있습니다. 투입 기간이 겹치지 않게 입력하세요.");
        }
    }

    /** 할당 계획 기간이 프로젝트 계획 기간(경계일 포함) 안에 있는지 검사. 위반 필드별 fieldError. */
    private void checkWithinProjectPeriod(Project project, AssignmentRequest req) {
        List<FieldErrorItem> errors = new ArrayList<>();
        if (req.getPlannedStart().isBefore(project.getPlannedStartDate())) {
            errors.add(new FieldErrorItem("plannedStart", "OUT_OF_PROJECT_PERIOD",
                    "프로젝트 계획 시작일(" + project.getPlannedStartDate() + ") 이전입니다.", req.getPlannedStart()));
        }
        if (req.getPlannedEnd().isAfter(project.getPlannedEndDate())) {
            errors.add(new FieldErrorItem("plannedEnd", "OUT_OF_PROJECT_PERIOD",
                    "프로젝트 계획 종료일(" + project.getPlannedEndDate() + ") 이후입니다.", req.getPlannedEnd()));
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_OUT_OF_PROJECT_PERIOD,
                    ErrorCode.ASSIGNMENT_OUT_OF_PROJECT_PERIOD.defaultMessage(), errors);
        }
    }

    private void apply(ProjectAssignment a, AssignmentRequest req) {
        String role = req.getRoleInProject();
        a.setRoleInProject(role == null || role.isBlank() ? "DEVELOPER" : role.trim());
        a.setAllocationRate(req.getAllocationRate() != null
                ? req.getAllocationRate().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("1.00"));
        a.setPlannedStart(req.getPlannedStart());
        a.setPlannedEnd(req.getPlannedEnd());
        a.setActualStart(req.getActualStart());
        a.setActualEnd(req.getActualEnd());
        String note = req.getNote();
        a.setNote(note == null || note.isBlank() ? null : note.trim());
    }
}
