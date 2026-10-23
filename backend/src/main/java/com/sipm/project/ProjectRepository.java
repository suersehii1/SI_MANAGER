package com.sipm.project;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    long countByStatus(ProjectStatus status);

    static Specification<Project> keyword(String pattern) {
        return (root, q, cb) -> pattern == null ? null : cb.or(
                cb.like(cb.lower(root.get("code")), pattern, '\\'),
                cb.like(cb.lower(root.get("name")), pattern, '\\'),
                cb.like(cb.lower(root.get("clientName")), pattern, '\\'));
    }

    static Specification<Project> status(ProjectStatus status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
