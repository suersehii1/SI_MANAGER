package com.sipm.issue.dto;

import com.sipm.issue.IssuePriority;
import com.sipm.issue.IssueStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record IssueRequest(
        @NotNull Long projectId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10000) String description,
        IssueStatus status,
        IssuePriority priority,
        @Min(0) @Max(100) Integer progress,
        Long assigneeId,
        LocalDate dueDate,
        // 수정 시 화면이 불러온 버전 (덮어쓰기 방지). 생성 시에는 쓰지 않음
        Long version) {
}
