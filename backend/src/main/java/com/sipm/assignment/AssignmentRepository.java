package com.sipm.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    /** 해당 프로젝트에 특정 역할(예: 'PM')로 할당된 적이 있는지 (WBS 편집 권한 판정용) */
    boolean existsByProjectIdAndUserIdAndRoleInProject(Long projectId, Long userId, String roleInProject);

    /** 프로젝트 투입 인원 수 (같은 사람이 기간을 나눠 여러 번 할당돼도 1명) */
    @Query("select count(distinct a.user.id) from ProjectAssignment a where a.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    /**
     * 같은 프로젝트에서 "같은 사람"의 할당 중 계획 기간이 [start, end]와 겹치는 것 (경계일 포함).
     * 같은 사람 = 같은 사용자 ID 이거나, 이메일이 같은 다른 계정 (동명이인은 이름으로 판단하지 않음).
     * email: 이메일이 없으면 "" (빈 문자열은 비교하지 않음)
     * excludeId: 수정 중인 자기 자신은 제외 (신규 등록이면 -1)
     * (PostgreSQL에서 null 파라미터 타입 추론 오류를 피하려고 null 대신 ""/-1을 쓴다)
     */
    @Query("select a from ProjectAssignment a join fetch a.user u where a.project.id = :projectId "
            + "and (u.id = :userId or (:email <> '' and lower(u.email) = lower(:email))) "
            + "and a.plannedStart <= :end and a.plannedEnd >= :start "
            + "and a.id <> :excludeId "
            + "order by a.plannedStart asc")
    List<ProjectAssignment> findOverlapping(@Param("projectId") Long projectId,
                                            @Param("userId") Long userId,
                                            @Param("email") String email,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end,
                                            @Param("excludeId") Long excludeId);

    @Query("select a from ProjectAssignment a join fetch a.user join fetch a.project where a.project.id = :projectId "
            + "order by a.plannedStart asc, a.user.name asc, a.id asc")
    List<ProjectAssignment> findByProjectIdFetch(@Param("projectId") Long projectId);

    @Query("select a from ProjectAssignment a join fetch a.user join fetch a.project")
    List<ProjectAssignment> findAllFetch();

    @Query("select a from ProjectAssignment a join fetch a.user join fetch a.project where a.project.id in :ids")
    List<ProjectAssignment> findByProjectIdsFetch(@Param("ids") Collection<Long> ids);

    @Query("select a.project.id, count(distinct a.user.id) from ProjectAssignment a where a.project.id in :ids group by a.project.id")
    List<Object[]> countByProjectIds(@Param("ids") Collection<Long> ids);
}
