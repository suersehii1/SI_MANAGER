package com.sipm.user;

import com.sipm.common.page.PageResponse;
import com.sipm.common.page.SortResolver;
import com.sipm.security.AppUserPrincipal;
import com.sipm.user.dto.UserCreateRequest;
import com.sipm.user.dto.UserResponse;
import com.sipm.user.dto.UserUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "username", "username", "name", "name", "createdAt", "createdAt");

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Boolean enabled,
                                           HttpServletRequest request,
                                           @AuthenticationPrincipal AppUserPrincipal me) {
        // ADMIN이 아니면(조회 전용) 아이디를 숨긴다 → 기본 정렬도 이름순
        boolean asAdmin = me.isAdmin();
        return userService.search(keyword, enabled, SortResolver.pageable(page, size,
                request.getParameterValues("sort"), SORTS, asAdmin ? "username,asc" : "name,asc", false), asAdmin);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest req) {
        UserResponse created = userService.create(req);
        return ResponseEntity.created(URI.create("/api/admin/users/" + created.id())).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest req,
                               @AuthenticationPrincipal AppUserPrincipal me) {
        return userService.update(id, req, me);
    }
}
