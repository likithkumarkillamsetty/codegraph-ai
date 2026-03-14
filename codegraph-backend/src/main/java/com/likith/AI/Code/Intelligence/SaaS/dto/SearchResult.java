package com.likith.AI.Code.Intelligence.SaaS.dto;

public class SearchResult {
    private String filePath;
    private String content;
    private Double similarity;

    public SearchResult() {}

    public SearchResult(String filePath, String content, Double similarity) {
        this.filePath = filePath;
        this.content = content;
        this.similarity = similarity;
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Double getSimilarity() { return similarity; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }
}