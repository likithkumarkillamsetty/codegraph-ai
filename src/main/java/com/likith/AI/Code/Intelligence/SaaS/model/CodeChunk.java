package com.likith.AI.Code.Intelligence.SaaS.model;

public class CodeChunk {

    private String filePath;
    private String chunkContent;

    public CodeChunk(String filePath, String chunkContent) {
        this.filePath = filePath;
        this.chunkContent = chunkContent;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getChunkContent() {
        return chunkContent;
    }
}