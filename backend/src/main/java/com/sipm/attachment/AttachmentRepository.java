package com.sipm.attachment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<IssueAttachment, Long> {

    @Query("select a from IssueAttachment a join fetch a.uploadedBy join fetch a.issue "
            + "where a.issue.id = :issueId order by a.createdAt asc, a.id asc")
    List<IssueAttachment> findByIssueIdFetch(@Param("issueId") Long issueId);

    @Query("select a from IssueAttachment a join fetch a.uploadedBy join fetch a.issue where a.id = :id")
    Optional<IssueAttachment> findByIdFetch(@Param("id") Long id);
}
