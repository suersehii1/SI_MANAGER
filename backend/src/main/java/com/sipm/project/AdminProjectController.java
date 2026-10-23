package com.sipm.project;

import com.sipm.common.page.PageResponse;
import com.sipm.common.page.SortResolver;
import com.sipm.project.dto.ProjectRequest;
import com.sipm.project.dto.ProjectResponse;
import com.sipm.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/** ADMIN 전용 프로젝트 CRUD (#7~#11). URL 레벨 + 클래스 레벨 @PreAuthorize 이중 방어. */
@RestController
@RequestMapping("/api/admin/projects")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProjectController {
    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "code", "code", "name", "name", "status", "status",
            "plannedStartDate", "plannedStartDate", "plannedEndDate", "plannedEndDate",
            "actualStartDate", "actualStartDate", "createdAt", "createdAt");

    private final ProjectService projectService;

    public AdminProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) ProjectStatus status,
                                              HttpServletRequest request) {
        return projectService.search(keyword, status, SortResolver.pageable(page, size,
                request.getParameterValues("sort"), SORTS, "plannedStartDate,desc", false));
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id) {
        return projectService.getResponse(id);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest req,
                                                  @AuthenticationPrincipal AppUserPrincipal me) {
        ProjectResponse created = projectService.create(req, me);
        return ResponseEntity.created(URI.create("/api/admin/projects/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest req) {
        return projectService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
