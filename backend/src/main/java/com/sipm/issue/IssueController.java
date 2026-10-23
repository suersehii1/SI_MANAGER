package com.sipm.issue;

import com.sipm.common.error.BusinessException;
import com.sipm.common.page.PageResponse;
import com.sipm.common.page.SortResolver;
import com.sipm.issue.dto.IssueDetail;
import com.sipm.issue.dto.IssueListItem;
import com.sipm.issue.dto.IssueRequest;
import com.sipm.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/issues")
public class IssueController {
    // API sort 필드 → 엔티티 속성. status/priority는 문자열이 아닌 rank 컬럼으로 정렬한다
    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "title", "title", "status", "statusRank", "priority", "priorityRank",
            "progress", "progress", "dueDate", "dueDate", "createdAt", "createdAt", "updatedAt", "updatedAt");

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping
    public PageResponse<IssueListItem> list(@RequestParam(required = false) Long projectId,
                                            @RequestParam(required = false) IssueStatus status,
                                            @RequestParam(required = false) IssuePriority priority,
                                            @RequestParam(required = false) Long assigneeId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size,
                                            HttpServletRequest request) {
        if (keyword != null && keyword.length() > 100) {
            throw BusinessException.validation("keyword", "Size", "검색어는 100자 이하여야 합니다.", keyword);
        }
        return issueService.search(projectId, status, priority, assigneeId, keyword,
                SortResolver.pageable(page, size, request.getParameterValues("sort"), SORTS, "createdAt,desc", true));
    }

    @GetMapping("/{id}")
    public IssueDetail get(@PathVariable Long id) {
        return issueService.get(id);
    }

    @PostMapping
    public ResponseEntity<IssueDetail> create(@Valid @RequestBody IssueRequest req,
                                              @AuthenticationPrincipal AppUserPrincipal me) {
        IssueDetail created = issueService.create(req, me);
        return ResponseEntity.created(URI.create("/api/issues/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public IssueDetail update(@PathVariable Long id, @Valid @RequestBody IssueRequest req) {
        return issueService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        issueService.delete(id, me);
        return ResponseEntity.noContent().build();
    }
}
