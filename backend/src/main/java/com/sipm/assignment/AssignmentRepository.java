package com.sipm.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    long countByProjectId(Long projectId);

    @Query("select a from ProjectAssignment a join fetch a.user join fetch a.project where a.project.id = :projectId "
            + "order by a.plannedStart asc, a.user.name asc, a.id asc")
    List<ProjectAssignment> findByProjectIdFetch(@Param("projectId") Long projectId);

    @Query("select a from ProjectAssignment a join fetch a.user join fetch a.project")
    List<ProjectAssignment> findAllFetch();

    @Query("select a from ProjectAssignment a join fetch a.user join fetch a.project where a.project.id in :ids")
    List<ProjectAssignment> findByProjectIdsFetch(@Param("ids") Collection<Long> ids);

    @Query("select a.project.id, count(a) from ProjectAssignment a where a.project.id in :ids group by a.project.id")
    List<Object[]> countByProjectIds(@Param("ids") Collection<Long> ids);
}
