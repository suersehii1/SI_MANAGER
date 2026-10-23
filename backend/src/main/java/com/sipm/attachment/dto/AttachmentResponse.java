package com.sipm.attachment.dto;

import com.sipm.attachment.IssueAttachment;
import com.sipm.user.dto.UserSummary;

import java.time.OffsetDateTime;

public record AttachmentResponse(Long id, Long issueId, String originalFilename, String contentType, long sizeBytes,
                                 UserSummary uploadedBy, OffsetDateTime createdAt) {
    public static AttachmentResponse from(IssueAttachment a) {
        return new AttachmentResponse(a.getId(), a.getIssue().getId(), a.getOriginalFilename(), a.getContentType(),
                a.getSizeBytes(), UserSummary.from(a.getUploadedBy()), a.getCreatedAt());
    }
}
