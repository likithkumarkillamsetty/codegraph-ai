package com.likith.AI.Code.Intelligence.SaaS.service;

import com.likith.AI.Code.Intelligence.SaaS.entity.Project;
import com.likith.AI.Code.Intelligence.SaaS.repository.ProjectRepository;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository repository;
    private final GitCloneService gitCloneService;

    public ProjectService(ProjectRepository repository,
                          GitCloneService gitCloneService) {
        this.repository = repository;
        this.gitCloneService = gitCloneService;
    }

    public Project getProjectById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public Project createProject(String name, String githubUrl) {

        try {
            Project project = new Project();
            project.setName(name);
            project.setGithubUrl(githubUrl);

            project = repository.save(project);

            String localPath = gitCloneService.cloneRepository(githubUrl, project.getId());
            project.setLocalPath(localPath);

            return repository.save(project);

        } catch (Exception e) {
            throw new RuntimeException("Failed to clone repository", e);
        }
    }
}