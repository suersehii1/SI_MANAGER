package com.sipm.issue;

import com.sipm.common.util.LikePatterns;
import org.springframework.data.jpa.domain.Specification;

public final class IssueSpecifications {
    private IssueSpecifications() {
    }

    public static Specification<Issue> projectId(Long projectId) {
        return (root, q, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Issue> status(IssueStatus status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Issue> priority(IssuePriority priority) {
        return (root, q, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Issue> assigneeId(Long assigneeId) {
        return (root, q, cb) -> assigneeId == null ? null : cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Issue> keyword(String keyword) {
        String pattern = LikePatterns.contains(keyword);
        return (root, q, cb) -> pattern == null ? null : cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\'));
    }
}
