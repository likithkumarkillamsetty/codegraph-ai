package com.likith.AI.Code.Intelligence.SaaS.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String githubUrl;

    @Column
    private String localPath;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}