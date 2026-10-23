package com.sipm.wbs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WbsItemRepository extends JpaRepository<WbsItem, Long> {

    /** Whole tree of a project in one query (assignee fetched); assembled in memory. */
    @Query("select w from WbsItem w left join fetch w.assignee where w.project.id = :projectId "
            + "order by w.sortOrder asc, w.id asc")
    List<WbsItem> findTreeByProjectId(@Param("projectId") Long projectId);
}
