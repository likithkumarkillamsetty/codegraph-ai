package com.likith.AI.Code.Intelligence.SaaS.model;

public class SourceFile {

    private String filePath;
    private String content;

    public SourceFile(String filePath, String content) {
        this.filePath = filePath;
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContent() {
        return content;
    }
}