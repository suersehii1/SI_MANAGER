package com.sipm.wbs;

import com.sipm.common.util.Times;
import com.sipm.security.AppUserPrincipal;
import com.sipm.wbs.dto.WbsItemRequest;
import com.sipm.wbs.dto.WbsItemResponse;
import com.sipm.wbs.dto.WbsMoveRequest;
import com.sipm.wbs.dto.WbsTreeResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

/** WBS API (#30~#34). 조회는 인증 사용자 모두, 쓰기는 ADMIN 또는 프로젝트 멤버(서비스에서 검사). */
@RestController
public class WbsController {
    private final WbsService wbsService;

    public WbsController(WbsService wbsService) {
        this.wbsService = wbsService;
    }

    @GetMapping("/api/projects/{projectId}/wbs")
    public WbsTreeResponse tree(@PathVariable Long projectId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
                                @AuthenticationPrincipal AppUserPrincipal me) {
        return wbsService.tree(projectId, Times.orToday(asOf), me);
    }

    @PostMapping("/api/projects/{projectId}/wbs")
    public ResponseEntity<WbsItemResponse> create(@PathVariable Long projectId, @Valid @RequestBody WbsItemRequest req,
                                                  @AuthenticationPrincipal AppUserPrincipal me) {
        WbsItemResponse created = wbsService.create(projectId, req, me, Times.today());
        return ResponseEntity.created(URI.create("/api/wbs/" + created.id())).body(created);
    }

    @PutMapping("/api/wbs/{id}")
    public WbsItemResponse update(@PathVariable Long id, @Valid @RequestBody WbsItemRequest req,
                                  @AuthenticationPrincipal AppUserPrincipal me) {
        return wbsService.update(id, req, me, Times.today());
    }

    @DeleteMapping("/api/wbs/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me,
                                       // 화면이 불러온 버전 (다른 사람이 먼저 수정했으면 409, 없으면 검사 안 함)
                                       @RequestParam(required = false) Long version) {
        wbsService.delete(id, me, version);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/wbs/{id}/move")
    public WbsTreeResponse move(@PathVariable Long id, @Valid @RequestBody WbsMoveRequest req,
                                @AuthenticationPrincipal AppUserPrincipal me) {
        return wbsService.move(id, req, me, Times.today());
    }
}
