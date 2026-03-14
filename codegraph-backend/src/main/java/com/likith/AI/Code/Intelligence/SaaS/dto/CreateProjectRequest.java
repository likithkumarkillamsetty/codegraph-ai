package com.likith.AI.Code.Intelligence.SaaS.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateProjectRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String githubUrl;

    public String getName() {
        return name;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }
}