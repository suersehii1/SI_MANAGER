package com.sipm.issue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

    @Override
    @EntityGraph(attributePaths = {"project", "assignee", "reporter"})
    Page<Issue> findAll(Specification<Issue> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "assignee", "reporter"})
    Optional<Issue> findWithRefsById(Long id);

    @Query("select i.id from Issue i where i.project.id = :projectId")
    List<Long> findIdsByProjectId(@Param("projectId") Long projectId);

    long countByStatusIn(Collection<IssueStatus> statuses);
}
