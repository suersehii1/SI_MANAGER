package com.sipm.wbs;

import com.sipm.assignment.AssignmentRepository;
import com.sipm.security.AppUserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * WBS 쓰기(추가/수정/삭제/이동) 권한 판정: ROLE_ADMIN 이거나 해당 프로젝트의 PM(할당 역할 = 'PM').
 * PM이 아닌 프로젝트 멤버(개발자 등)는 조회만 가능하다.
 * 조회는 모든 인증 사용자에게 허용되므로 여기서 검사하지 않는다.
 */
@Service
public class WbsPermissionService {
    /** 인원 할당 역할 중 WBS 편집 권한을 갖는 역할 (프론트 PROJECT_ROLES의 'PM') */
    public static final String PM_ROLE = "PM";

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
        return assignmentRepository.existsByProjectIdAndUserIdAndRoleInProject(projectId, user.getId(), PM_ROLE);
    }

    /** 권한이 없으면 AccessDeniedException → 전역 핸들러가 403 FORBIDDEN으로 변환. */
    public void checkEdit(Long projectId, AppUserPrincipal user) {
        if (!canEdit(projectId, user)) {
            throw new AccessDeniedException("WBS edit not allowed");
        }
    }
}
