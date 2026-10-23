package com.sipm.project;

import com.sipm.assignment.AssignmentRepository;
import com.sipm.assignment.ProjectAssignment;
import com.sipm.common.error.BusinessException;
import com.sipm.project.dto.ProjectSummary;
import com.sipm.user.dto.UserSummary;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 인증 사용자용 lookup: 프로젝트 목록(#5), 프로젝트 멤버(#35, WBS 담당자 선택용). */
@RestController
@RequestMapping("/api/projects")
public class ProjectLookupController {
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final AssignmentRepository assignmentRepository;

    public ProjectLookupController(ProjectService projectService, ProjectRepository projectRepository,
                                   AssignmentRepository assignmentRepository) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @GetMapping
    public List<ProjectSummary> list() {
        return projectService.lookup();
    }

    @GetMapping("/{projectId}/members")
    @Transactional(readOnly = true)
    public List<UserSummary> members(@PathVariable Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw BusinessException.notFound("프로젝트");
        }
        List<UserSummary> members = new ArrayList<>();
        for (ProjectAssignment a : assignmentRepository.findByProjectIdFetch(projectId)) {
            members.add(UserSummary.from(a.getUser()));
        }
        members.sort(Comparator.comparing(UserSummary::name));
        return members;
    }
}
