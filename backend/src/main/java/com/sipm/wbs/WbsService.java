package com.sipm.wbs;

import com.sipm.assignment.AssignmentRepository;
import com.sipm.common.error.BusinessException;
import com.sipm.common.error.OptimisticLocks;
import com.sipm.common.error.ErrorCode;
import com.sipm.common.error.FieldErrorItem;
import com.sipm.project.Project;
import com.sipm.project.ProjectService;
import com.sipm.project.dto.ProjectSummary;
import com.sipm.security.AppUserPrincipal;
import com.sipm.user.User;
import com.sipm.user.UserService;
import com.sipm.user.dto.UserSummary;
import com.sipm.wbs.dto.WbsItemRequest;
import com.sipm.wbs.dto.WbsItemResponse;
import com.sipm.wbs.dto.WbsMoveRequest;
import com.sipm.wbs.dto.WbsTreeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WBS CRUD / 이동 / 재정렬 (specs-wbs §W3, §W4).
 *
 * 핵심 규칙
 * - 트리는 프로젝트 단위로 한 번에 읽어 메모리에서 조립한다(N+1 없음). 집계는 {@link WbsRollupCalculator}.
 * - 형제 간 sort_order는 항상 0..n-1 연속으로 유지한다(삽입/이동/삭제 후 재번호).
 * - 깊이는 최대 5. 이동 시 "새 부모 레벨 + 이동하는 서브트리 높이"로 검사한다.
 * - 리프만 기간/진척률/상태를 직접 입력. 자식이 있는 노드의 해당 필드 수정 요청은 무시한다.
 * - 쓰기 권한: ADMIN 또는 프로젝트 멤버 ({@link WbsPermissionService}).
 */
@Service
public class WbsService {
    private final WbsItemRepository wbsRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final AssignmentRepository assignmentRepository;
    private final WbsPermissionService permissionService;

    public WbsService(WbsItemRepository wbsRepository, ProjectService projectService, UserService userService,
                      AssignmentRepository assignmentRepository, WbsPermissionService permissionService) {
        this.wbsRepository = wbsRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.assignmentRepository = assignmentRepository;
        this.permissionService = permissionService;
    }

    // ===================== 조회 =====================

    @Transactional(readOnly = true)
    public WbsTreeResponse tree(Long projectId, LocalDate asOf, AppUserPrincipal me) {
        Project project = projectService.get(projectId);
        return buildTree(project, asOf, me);
    }

    // ===================== 생성 =====================

    @Transactional
    public WbsItemResponse create(Long projectId, WbsItemRequest req, AppUserPrincipal me, LocalDate asOf) {
        Project project = projectService.get(projectId);
        permissionService.checkEdit(projectId, me);

        List<WbsItem> all = wbsRepository.findTreeByProjectId(projectId);
        WbsRollupCalculator.Result tree = calculate(all);
        Map<Long, WbsRollupCalculator.Node> nodes = indexNodes(tree);

        // 부모 검증: 같은 프로젝트에 존재해야 하고, 새 노드의 깊이(부모 레벨+1)가 5 이하
        Long parentId = req.getParentId();
        if (parentId != null) {
            WbsRollupCalculator.Node parent = nodes.get(parentId);
            if (parent == null) {
                throw new BusinessException(ErrorCode.WBS_INVALID_PARENT);
            }
            if (parent.level + 1 > WbsRollupCalculator.MAX_DEPTH) {
                throw new BusinessException(ErrorCode.WBS_DEPTH_EXCEEDED);
            }
        }

        WbsItem item = new WbsItem();
        item.setProject(project);
        item.setParentId(parentId);
        applyCommonFields(item, req, projectId);
        // 새 노드는 항상 리프 → 기간/진척/상태 입력 반영
        applyLeafFields(item, req, project);

        // 삽입 위치: insertAfterId 형제 바로 뒤, 없으면 마지막
        List<WbsItem> siblings = siblingsOf(all, parentId, null);
        int index = siblings.size();
        if (req.getInsertAfterId() != null) {
            for (int i = 0; i < siblings.size(); i++) {
                if (siblings.get(i).getId().equals(req.getInsertAfterId())) {
                    index = i + 1;
                    break;
                }
            }
        }
        item.setSortOrder(index);
        item = wbsRepository.save(item);
        siblings.add(index, item);
        renumber(siblings);
        wbsRepository.flush();

        return itemResponse(project, item.getId(), asOf);
    }

    // ===================== 수정 =====================

    @Transactional
    public WbsItemResponse update(Long id, WbsItemRequest req, AppUserPrincipal me, LocalDate asOf) {
        WbsItem item = wbsRepository.findById(id).orElseThrow(() -> BusinessException.notFound("WBS 항목"));
        Project project = item.getProject();
        permissionService.checkEdit(project.getId(), me);
        OptimisticLocks.check(item.getVersion(), req.getVersion()); // 다른 사람이 먼저 수정했으면 409

        boolean hasChildren = false;
        for (WbsItem w : wbsRepository.findTreeByProjectId(project.getId())) {
            if (id.equals(w.getParentId())) {
                hasChildren = true;
                break;
            }
        }

        applyCommonFields(item, req, project.getId());
        // 자식이 있는 노드는 기간/진척/상태가 집계값이므로 요청 값을 무시한다
        if (!hasChildren) {
            applyLeafFields(item, req, project);
        }
        wbsRepository.flush();
        return itemResponse(project, id, asOf);
    }

    // ===================== 삭제 =====================

    @Transactional
    public void delete(Long id, AppUserPrincipal me) {
        WbsItem item = wbsRepository.findById(id).orElseThrow(() -> BusinessException.notFound("WBS 항목"));
        Long projectId = item.getProject().getId();
        permissionService.checkEdit(projectId, me);

        List<WbsItem> all = wbsRepository.findTreeByProjectId(projectId);
        WbsRollupCalculator.Result tree = calculate(all);
        Map<Long, WbsItem> entities = indexEntities(all);

        // 하위 트리를 자식부터(후위 순서) 명시적으로 삭제 — DB CASCADE에 의존하지 않고 영속성 컨텍스트와 일치시킨다
        List<Long> subtree = new ArrayList<>();
        collectPostOrder(indexNodes(tree).get(id), subtree);
        for (Long sid : subtree) {
            wbsRepository.delete(entities.get(sid));
        }
        wbsRepository.flush();

        // 남은 형제 재번호
        renumber(siblingsOf(all, item.getParentId(), id));
        wbsRepository.flush();
    }

    // ===================== 이동 =====================

    /** 부모 변경/순서 변경. 들여쓰기·내어쓰기·위/아래 이동 모두 이 API 하나로 처리한다. */
    @Transactional
    public WbsTreeResponse move(Long id, WbsMoveRequest req, AppUserPrincipal me, LocalDate asOf) {
        WbsItem item = wbsRepository.findById(id).orElseThrow(() -> BusinessException.notFound("WBS 항목"));
        Project project = item.getProject();
        permissionService.checkEdit(project.getId(), me);

        List<WbsItem> all = wbsRepository.findTreeByProjectId(project.getId());
        WbsRollupCalculator.Result tree = calculate(all);
        Map<Long, WbsRollupCalculator.Node> nodes = indexNodes(tree);
        WbsRollupCalculator.Node self = nodes.get(id);

        Long newParentId = req.parentId();
        int newParentLevel = 0; // 루트로 이동하면 부모 레벨 0
        if (newParentId != null) {
            // 자기 자신 또는 자신의 하위 노드를 부모로 지정하면 순환 → 400 WBS_INVALID_MOVE
            if (newParentId.equals(id) || isDescendant(self, newParentId)) {
                throw new BusinessException(ErrorCode.WBS_INVALID_MOVE);
            }
            WbsRollupCalculator.Node parent = nodes.get(newParentId);
            if (parent == null) {
                throw new BusinessException(ErrorCode.WBS_INVALID_PARENT);
            }
            newParentLevel = parent.level;
        }
        // 깊이 검사: 새 부모 레벨 + 이동하는 서브트리 높이 ≤ 5
        if (newParentLevel + WbsRollupCalculator.subtreeHeight(self) > WbsRollupCalculator.MAX_DEPTH) {
            throw new BusinessException(ErrorCode.WBS_DEPTH_EXCEEDED);
        }

        Long oldParentId = item.getParentId();
        // 1) 기존 형제 목록에서 제거 후 재번호
        List<WbsItem> oldSiblings = siblingsOf(all, oldParentId, id);
        renumber(oldSiblings);
        // 2) 새 형제 목록에 index 위치로 삽입 후 재번호 (같은 부모 내 이동이면 1)의 결과 목록을 그대로 사용)
        List<WbsItem> newSiblings = equalsNullable(oldParentId, newParentId)
                ? oldSiblings : siblingsOf(all, newParentId, id);
        int index = Math.min(req.index(), newSiblings.size());
        item.setParentId(newParentId);
        newSiblings.add(index, item);
        renumber(newSiblings);
        wbsRepository.flush();

        return buildTree(project, asOf, me);
    }

    // ===================== 내부 헬퍼 =====================

    /** 이름/설명/산출물/담당자/가중치: 모든 노드에 반영. */
    private void applyCommonFields(WbsItem item, WbsItemRequest req, Long projectId) {
        item.setName(req.getName().trim());
        item.setDescription(blankToNull(req.getDescription()));
        item.setDeliverable(blankToNull(req.getDeliverable()));
        // NUMERIC(5,2)와 동일한 scale로 맞춰 응답에서도 1.00 형태 유지
        item.setWeight(req.getWeight() != null ? req.getWeight().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("1.00"));
        item.setAssignee(resolveAssignee(req.getAssigneeId(), projectId));
    }

    /**
     * 리프 전용 필드(기간/진척/상태) 반영 + 리프 입력 규칙 (specs-wbs W3):
     * 1) 이미 DONE인 항목에 DONE + 진척 < 100 이 오면(= 진척률만 낮춤) → IN_PROGRESS로 전환, 진척률 유지
     * 2) 상태를 DONE으로 바꾸는 요청(신규 포함) → 진척 100 강제
     * 3) 진척 100 & NOT_STARTED → DONE / 0 < 진척 < 100 & NOT_STARTED → IN_PROGRESS (ON_HOLD는 유지)
     * 계획 기간은 프로젝트 계획 기간 안이어야 한다(WBS_OUT_OF_PROJECT_PERIOD).
     */
    private void applyLeafFields(WbsItem item, WbsItemRequest req, Project project) {
        List<FieldErrorItem> errors = new ArrayList<>();
        if (req.getPlannedStart() != null && (req.getPlannedStart().isBefore(project.getPlannedStartDate())
                || req.getPlannedStart().isAfter(project.getPlannedEndDate()))) {
            errors.add(new FieldErrorItem("plannedStart", "OUT_OF_PROJECT_PERIOD",
                    "프로젝트 계획 기간(" + project.getPlannedStartDate() + " ~ " + project.getPlannedEndDate()
                            + ")을 벗어났습니다.", req.getPlannedStart()));
        }
        if (req.getPlannedEnd() != null && (req.getPlannedEnd().isBefore(project.getPlannedStartDate())
                || req.getPlannedEnd().isAfter(project.getPlannedEndDate()))) {
            errors.add(new FieldErrorItem("plannedEnd", "OUT_OF_PROJECT_PERIOD",
                    "프로젝트 계획 기간(" + project.getPlannedStartDate() + " ~ " + project.getPlannedEndDate()
                            + ")을 벗어났습니다.", req.getPlannedEnd()));
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.WBS_OUT_OF_PROJECT_PERIOD,
                    ErrorCode.WBS_OUT_OF_PROJECT_PERIOD.defaultMessage(), errors);
        }

        // 신규 항목(id 없음)은 이전 상태가 없다
        WbsStatus previous = item.getId() == null ? null : item.getStatus();
        WbsStatus status = req.getStatus() != null ? req.getStatus() : WbsStatus.NOT_STARTED;
        int progress = req.getProgress() != null ? req.getProgress() : 0;
        if (status == WbsStatus.DONE && previous == WbsStatus.DONE && progress < 100) {
            // 그리드에서 완료 항목의 진척률만 낮춘 경우: 100으로 되돌리지 않고 진행 중으로 전환
            status = WbsStatus.IN_PROGRESS;
        } else if (status == WbsStatus.DONE) {
            progress = 100;
        } else if (status == WbsStatus.NOT_STARTED && progress == 100) {
            status = WbsStatus.DONE;
        } else if (status == WbsStatus.NOT_STARTED && progress > 0) {
            status = WbsStatus.IN_PROGRESS;
        }
        item.setStatus(status);
        item.setProgress(progress);
        item.setPlannedStart(req.getPlannedStart());
        item.setPlannedEnd(req.getPlannedEnd());
        item.setActualStart(req.getActualStart());
        item.setActualEnd(req.getActualEnd());
    }

    /** 담당자: 존재(404) → 활성(400 USER_DISABLED) → 프로젝트 멤버(400 WBS_ASSIGNEE_NOT_MEMBER). */
    private User resolveAssignee(Long assigneeId, Long projectId) {
        if (assigneeId == null) {
            return null;
        }
        User user = userService.getEnabled(assigneeId);
        if (!assignmentRepository.existsByProjectIdAndUserId(projectId, assigneeId)) {
            throw new BusinessException(ErrorCode.WBS_ASSIGNEE_NOT_MEMBER,
                    ErrorCode.WBS_ASSIGNEE_NOT_MEMBER.defaultMessage(),
                    List.of(new FieldErrorItem("assigneeId", "NOT_MEMBER",
                            ErrorCode.WBS_ASSIGNEE_NOT_MEMBER.defaultMessage(), assigneeId)));
        }
        return user;
    }

    /** 같은 부모를 가진 형제 목록(sort_order 순). excludeId는 목록에서 제외. */
    private static List<WbsItem> siblingsOf(List<WbsItem> all, Long parentId, Long excludeId) {
        List<WbsItem> siblings = new ArrayList<>();
        for (WbsItem w : all) {
            if (equalsNullable(w.getParentId(), parentId) && !w.getId().equals(excludeId)) {
                siblings.add(w);
            }
        }
        siblings.sort((a, b) -> a.getSortOrder() != b.getSortOrder()
                ? Integer.compare(a.getSortOrder(), b.getSortOrder()) : Long.compare(a.getId(), b.getId()));
        return siblings;
    }

    /** 형제 sort_order를 0..n-1로 다시 매긴다 (변경된 값만 dirty 처리됨). */
    private static void renumber(List<WbsItem> siblings) {
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getSortOrder() != i) {
                siblings.get(i).setSortOrder(i);
            }
        }
    }

    private static boolean isDescendant(WbsRollupCalculator.Node node, Long targetId) {
        for (WbsRollupCalculator.Node c : node.children) {
            if (c.id.equals(targetId) || isDescendant(c, targetId)) {
                return true;
            }
        }
        return false;
    }

    private static void collectPostOrder(WbsRollupCalculator.Node node, List<Long> out) {
        for (WbsRollupCalculator.Node c : node.children) {
            collectPostOrder(c, out);
        }
        out.add(node.id);
    }

    private static boolean equalsNullable(Long a, Long b) {
        return a == null ? b == null : a.equals(b);
    }

    private static WbsRollupCalculator.Result calculate(List<WbsItem> items) {
        List<WbsRollupCalculator.Node> nodes = new ArrayList<>();
        for (WbsItem w : items) {
            nodes.add(new WbsRollupCalculator.Node(w.getId(), w.getParentId(), w.getSortOrder(), w.getWeight(),
                    w.getStatus(), w.getPlannedStart(), w.getPlannedEnd(), w.getActualStart(), w.getActualEnd(),
                    w.getProgress()));
        }
        return WbsRollupCalculator.calculate(nodes);
    }

    private static Map<Long, WbsRollupCalculator.Node> indexNodes(WbsRollupCalculator.Result tree) {
        Map<Long, WbsRollupCalculator.Node> map = new HashMap<>();
        for (WbsRollupCalculator.Node n : tree.ordered()) {
            map.put(n.id, n);
        }
        return map;
    }

    private static Map<Long, WbsItem> indexEntities(List<WbsItem> items) {
        Map<Long, WbsItem> map = new HashMap<>();
        for (WbsItem w : items) {
            map.put(w.getId(), w);
        }
        return map;
    }

    /** 트리 전체를 다시 계산해 응답(DFS 순서)을 만든다. */
    private WbsTreeResponse buildTree(Project project, LocalDate asOf, AppUserPrincipal me) {
        List<WbsItem> all = wbsRepository.findTreeByProjectId(project.getId());
        WbsRollupCalculator.Result tree = calculate(all);
        Map<Long, WbsItem> entities = indexEntities(all);
        List<WbsItemResponse> items = new ArrayList<>();
        for (WbsRollupCalculator.Node n : tree.ordered()) {
            items.add(toResponse(project.getId(), entities.get(n.id), n, asOf));
        }
        return new WbsTreeResponse(ProjectSummary.from(project), asOf,
                permissionService.canEdit(project.getId(), me), tree.overallProgress(), items.size(), items);
    }

    /** 단일 항목 응답(집계 반영). */
    private WbsItemResponse itemResponse(Project project, Long id, LocalDate asOf) {
        List<WbsItem> all = wbsRepository.findTreeByProjectId(project.getId());
        WbsRollupCalculator.Result tree = calculate(all);
        WbsRollupCalculator.Node node = indexNodes(tree).get(id);
        return toResponse(project.getId(), indexEntities(all).get(id), node, asOf);
    }

    private static WbsItemResponse toResponse(Long projectId, WbsItem w, WbsRollupCalculator.Node n, LocalDate asOf) {
        return new WbsItemResponse(w.getId(), projectId, w.getParentId(), n.wbsCode, n.level, w.getSortOrder(),
                w.getName(), w.getDescription(), w.getDeliverable(), UserSummary.from(w.getAssignee()),
                n.status, w.getWeight(), n.plannedStart, n.plannedEnd, n.actualStart, n.actualEnd,
                n.progress, WbsRollupCalculator.plannedDays(n), WbsRollupCalculator.isDelayed(n, asOf),
                n.hasChildren(), n.children.size(), n.hasChildren(), w.getUpdatedAt(), w.getVersion());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
