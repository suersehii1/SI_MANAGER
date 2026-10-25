package com.sipm.assignment;

import com.sipm.assignment.dto.AssignmentRequest;
import com.sipm.assignment.dto.AssignmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 할당 관리 (#12~#15). 조회는 로그인 사용자 모두, 등록/수정/삭제는 ADMIN만 (URL 규칙 + 메서드 @PreAuthorize). */
@RestController
@RequestMapping("/api/admin/projects/{id}/assignments")
public class AdminAssignmentController {
    private final AssignmentService assignmentService;

    public AdminAssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<AssignmentResponse> list(@PathVariable("id") Long projectId) {
        return assignmentService.list(projectId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AssignmentResponse> create(@PathVariable("id") Long projectId,
                                                     @Valid @RequestBody AssignmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.create(projectId, req));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{assignmentId}")
    public AssignmentResponse update(@PathVariable("id") Long projectId, @PathVariable Long assignmentId,
                                     @Valid @RequestBody AssignmentRequest req) {
        return assignmentService.update(projectId, assignmentId, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long projectId, @PathVariable Long assignmentId,
                                       // 화면이 불러온 버전 (다른 사람이 먼저 수정했으면 409, 없으면 검사 안 함)
                                       @RequestParam(required = false) Long version) {
        assignmentService.delete(projectId, assignmentId, version);
        return ResponseEntity.noContent().build();
    }
}
