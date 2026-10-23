package com.sipm.issue.dto;

import com.sipm.issue.Issue;
import com.sipm.issue.IssuePriority;
import com.sipm.issue.IssueStatus;
import com.sipm.user.dto.UserSummary;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record IssueListItem(Long id, Long projectId, String projectCode, String projectName, String title,
                            IssueStatus status, IssuePriority priority, int progress, UserSummary assignee,
                            UserSummary reporter, LocalDate dueDate, long attachmentCount,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static IssueListItem from(Issue i) {
        return new IssueListItem(i.getId(), i.getProject().getId(), i.getProject().getCode(), i.getProject().getName(),
                i.getTitle(), i.getStatus(), i.getPriority(), i.getProgress(), UserSummary.from(i.getAssignee()),
                UserSummary.from(i.getReporter()), i.getDueDate(), i.getAttachmentCount(), i.getCreatedAt(),
                i.getUpdatedAt());
    }
}
