package com.sipm.issue;

import com.sipm.project.Project;
import com.sipm.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "issues")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 낙관적 잠금 버전: 저장할 때마다 1씩 증가. 다른 사람이 먼저 저장했는지 판단하는 데 쓴다. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 10000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IssueStatus status = IssueStatus.OPEN;

    @Column(name = "status_rank", nullable = false)
    private short statusRank = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private IssuePriority priority = IssuePriority.MEDIUM;

    @Column(name = "priority_rank", nullable = false)
    private short priorityRank = 2;

    @Column(name = "progress", nullable = false)
    private int progress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // 목록 조회 시 첨부 수를 별도 쿼리 없이 서브쿼리로 함께 읽는다
    @Formula("(select count(*) from issue_attachments a where a.issue_id = id)")
    private Long attachmentCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Issue() {
    }

    /**
     * 정렬용 rank 컬럼을 enum에서 채운다. status/priority를 문자열 정렬하면 순서가 틀리므로
     * (예: CRITICAL < HIGH < LOW < MEDIUM) 정렬은 status_rank/priority_rank로 한다.
     */
    @PrePersist
    @PreUpdate
    void syncRanks() {
        this.statusRank = status.rank();
        this.priorityRank = priority.rank();
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) {
        this.status = status;
        this.statusRank = status.rank();
    }
    public short getStatusRank() { return statusRank; }
    public IssuePriority getPriority() { return priority; }
    public void setPriority(IssuePriority priority) {
        this.priority = priority;
        this.priorityRank = priority.rank();
    }
    public short getPriorityRank() { return priorityRank; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public long getAttachmentCount() { return attachmentCount == null ? 0 : attachmentCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public Long getVersion() { return version; }
}
