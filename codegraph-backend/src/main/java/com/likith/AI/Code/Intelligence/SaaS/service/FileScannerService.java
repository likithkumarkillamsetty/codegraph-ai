package com.likith.AI.Code.Intelligence.SaaS.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileScannerService {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".java", ".properties", ".yml", ".yaml", ".xml", ".json", ".md", ".txt", ".sql",
            ".tsx", ".ts", ".jsx", ".js", ".css"
    );

    private static final List<String> IGNORED_DIRS = List.of(
            ".git", "node_modules", "target", "build", ".idea", "__pycache__", "dist", ".vite"
    );

    public List<File> scanJavaFiles(String projectPath) {
        List<File> files = new ArrayList<>();
        File root = new File(projectPath);
        scanRecursively(root, files);
        return files;
    }

    private void scanRecursively(File file, List<File> files) {
        if (file == null || !file.exists()) return;

        if (file.isDirectory()) {
            if (IGNORED_DIRS.contains(file.getName())) return;
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    scanRecursively(child, files);
                }
            }
        } else {
            String name = file.getName();
            for (String ext : SUPPORTED_EXTENSIONS) {
                if (name.endsWith(ext)) {
                    files.add(file);
                    break;
                }
            }
        }
    }
}