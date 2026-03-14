package com.likith.AI.Code.Intelligence.SaaS.service;

import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class GitCloneService {

    private static final String STORAGE_DIR = "storage";

    public String cloneRepository(String githubUrl, Long projectId) throws Exception {

        File storageDir = new File(STORAGE_DIR);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        String projectPath = STORAGE_DIR + File.separator + projectId;
        File projectDir = new File(projectPath);

        if (projectDir.exists()) {
            return projectPath; // already cloned
        }

        Git.cloneRepository()
                .setURI(githubUrl)
                .setDirectory(projectDir)
                .call();

        return projectPath;
    }
}