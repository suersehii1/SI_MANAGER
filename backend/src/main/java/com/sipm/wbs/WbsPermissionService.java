package com.sipm.wbs;

import com.sipm.assignment.AssignmentRepository;
import com.sipm.security.AppUserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * WBS 쓰기 권한 판정: ROLE_ADMIN 이거나 해당 프로젝트에 할당(project_assignments)된 사용자.
 * 조회는 모든 인증 사용자에게 허용되므로 여기서 검사하지 않는다.
 */
@Service
public class WbsPermissionService {
    private final AssignmentRepository assignmentRepository;

    public WbsPermissionService(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    public boolean canEdit(Long projectId, AppUserPrincipal user) {
        if (user == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        return assignmentRepository.existsByProjectIdAndUserId(projectId, user.getId());
    }

    /** 권한이 없으면 AccessDeniedException → 전역 핸들러가 403 FORBIDDEN으로 변환. */
    public void checkEdit(Long projectId, AppUserPrincipal user) {
        if (!canEdit(projectId, user)) {
            throw new AccessDeniedException("WBS edit not allowed");
        }
    }
}
