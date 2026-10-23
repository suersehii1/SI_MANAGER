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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** ADMIN 전용 할당 관리 (#12~#15). */
@RestController
@RequestMapping("/api/admin/projects/{id}/assignments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAssignmentController {
    private final AssignmentService assignmentService;

    public AdminAssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<AssignmentResponse> list(@PathVariable("id") Long projectId) {
        return assignmentService.list(projectId);
    }

    @PostMapping
    public ResponseEntity<AssignmentResponse> create(@PathVariable("id") Long projectId,
                                                     @Valid @RequestBody AssignmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.create(projectId, req));
    }

    @PutMapping("/{assignmentId}")
    public AssignmentResponse update(@PathVariable("id") Long projectId, @PathVariable Long assignmentId,
                                     @Valid @RequestBody AssignmentRequest req) {
        return assignmentService.update(projectId, assignmentId, req);
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long projectId, @PathVariable Long assignmentId) {
        assignmentService.delete(projectId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
