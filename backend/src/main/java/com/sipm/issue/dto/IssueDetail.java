package com.sipm.issue.dto;

import com.sipm.attachment.dto.AttachmentResponse;
import com.sipm.issue.Issue;
import com.sipm.issue.IssuePriority;
import com.sipm.issue.IssueStatus;
import com.sipm.user.dto.UserSummary;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record IssueDetail(Long id, Long projectId, String projectCode, String projectName, String title,
                          IssueStatus status, IssuePriority priority, int progress, UserSummary assignee,
                          UserSummary reporter, LocalDate dueDate, long attachmentCount,
                          OffsetDateTime createdAt, OffsetDateTime updatedAt,
                          String description, List<AttachmentResponse> attachments, Long version) {
    public static IssueDetail from(Issue i, List<AttachmentResponse> attachments) {
        return new IssueDetail(i.getId(), i.getProject().getId(), i.getProject().getCode(), i.getProject().getName(),
                i.getTitle(), i.getStatus(), i.getPriority(), i.getProgress(), UserSummary.from(i.getAssignee()),
                UserSummary.from(i.getReporter()), i.getDueDate(), attachments.size(), i.getCreatedAt(),
                i.getUpdatedAt(), i.getDescription(), attachments, i.getVersion());
    }
}
