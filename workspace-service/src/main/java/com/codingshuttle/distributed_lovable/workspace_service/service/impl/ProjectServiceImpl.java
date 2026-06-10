package com.codingshuttle.distributed_lovable.workspace_service.service.impl;

import com.codingshuttle.distributed_lovable.common_lib.dto.PlanDto;
import com.codingshuttle.distributed_lovable.common_lib.enums.ProjectPermission;
import com.codingshuttle.distributed_lovable.common_lib.enums.ProjectRole;
import com.codingshuttle.distributed_lovable.common_lib.error.BadRequestException;
import com.codingshuttle.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.codingshuttle.distributed_lovable.common_lib.security.AuthUtil;
import com.codingshuttle.distributed_lovable.workspace_service.client.AccountClient;
import com.codingshuttle.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.codingshuttle.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.codingshuttle.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;
import com.codingshuttle.distributed_lovable.workspace_service.entity.Project;
import com.codingshuttle.distributed_lovable.workspace_service.entity.ProjectMember;
import com.codingshuttle.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.codingshuttle.distributed_lovable.workspace_service.mapper.ProjectMapper;
import com.codingshuttle.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.codingshuttle.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.codingshuttle.distributed_lovable.workspace_service.security.SecurityExpressions;
import com.codingshuttle.distributed_lovable.workspace_service.service.ProjectService;
import com.codingshuttle.distributed_lovable.workspace_service.service.ProjectTemplateService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@Transactional
public class ProjectServiceImpl implements ProjectService {

    ProjectRepository projectRepository;
    ProjectMapper projectMapper;
    ProjectMemberRepository projectMemberRepository;
    AuthUtil authUtil;
    ProjectTemplateService projectTemplateService;
    AccountClient accountClient;
    SecurityExpressions securityExpressions;

    @Override
    public ProjectResponse createProject(ProjectRequest request) {

        if(!canCreateNewProject()){
            throw new BadRequestException("User cannot create a New Project with current plan, Upgrade plan now.");
        }

        Long ownerUserId = authUtil.getCurrentUserId();

        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();
        project = projectRepository.save(project);

        ProjectMemberId projectMemberId = new ProjectMemberId(project.getId(), ownerUserId);
        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(ProjectRole.OWNER)
                .acceptedAt(Instant.now())
                .invitedAt(Instant.now())
                .project(project)
                .build();
        projectMemberRepository.save(projectMember);

        projectTemplateService.initializeProjectFromTemplate(project.getId());

        return projectMapper.toProjectResponse(project);
    }

    @Override
    public List<ProjectSummaryResponse> getUserProjects() {
        Long userId = authUtil.getCurrentUserId();

        var projectsWithRole = projectRepository.findAllAccessibleByUser(userId);
        return projectsWithRole.stream()
                .map(p -> projectMapper.toProjectSummaryResponse(p.getProject(),p.getRole()))
                .toList();
    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectSummaryResponse getUserProjectById(Long projectId) {
        Long userId = authUtil.getCurrentUserId();

        var projectWithRole = projectRepository.findAccessibleProjectByIdWithRole(projectId,userId)
                .orElseThrow(() -> new BadRequestException("Project not found"));

        return projectMapper.toProjectSummaryResponse(projectWithRole.getProject(),projectWithRole.getRole());
    }


    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectsById(id,userId);

        project.setName(request.name());
        project = projectRepository.save(project);

        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canDeleteProject(#projectId)")
    public void softDelete(Long id) {
        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectsById(id,userId);

        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
    }

    @Override
    public boolean hasPermission(Long projectId, ProjectPermission permission) {
        return securityExpressions.hasPermission(projectId,permission);
    }

    // INTERNAL FUNCTIONS

    public Project getAccessibleProjectsById(Long projectId,Long userId)
    {
        return projectRepository.findAccessibleProjectById(projectId,userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project" , projectId.toString()));
    }

    private boolean canCreateNewProject(){
        Long userId = authUtil.getCurrentUserId();
        if(userId == null){
            return false;
        }
        PlanDto plan = accountClient.getCurrentSubscribedPLanByUser();

        int maxAllowed = plan.maxProjects();
        int ownedCount = projectMemberRepository.countProjectsOwnedByUser(userId);

        return ownedCount < maxAllowed;
    }
}
