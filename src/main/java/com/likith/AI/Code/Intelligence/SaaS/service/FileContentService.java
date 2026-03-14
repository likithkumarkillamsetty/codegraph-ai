package com.likith.AI.Code.Intelligence.SaaS.service;

import com.likith.AI.Code.Intelligence.SaaS.model.SourceFile;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileContentService {

    public List<SourceFile> readJavaFiles(List<File> javaFiles) {

        List<SourceFile> sourceFiles = new ArrayList<>();

        for (File file : javaFiles) {
            try {
                String content = Files.readString(file.toPath());
                sourceFiles.add(
                        new SourceFile(file.getAbsolutePath(), content)
                );
            } catch (Exception e) {
                System.out.println("Failed to read: " + file.getAbsolutePath());
            }
        }

        return sourceFiles;
    }
}