package com.likith.AI.Code.Intelligence.SaaS.service;

import com.likith.AI.Code.Intelligence.SaaS.model.CodeChunk;
import com.likith.AI.Code.Intelligence.SaaS.model.SourceFile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CodeChunkService {

    private static final int MAX_CHUNK_SIZE = 3000;

    public List<CodeChunk> chunkSourceFiles(List<SourceFile> sourceFiles) {

        List<CodeChunk> chunks = new ArrayList<>();

        for (SourceFile file : sourceFiles) {

            String content = file.getContent();

            String[] methodSplits = content.split("(?=public |private |protected )");

            for (String part : methodSplits) {

                if (part.length() > MAX_CHUNK_SIZE) {
                    chunks.addAll(splitLargeChunk(file.getFilePath(), part));
                } else {
                    chunks.add(new CodeChunk(file.getFilePath(), part));
                }
            }
        }

        return chunks;
    }

    private List<CodeChunk> splitLargeChunk(String filePath, String content) {

        List<CodeChunk> smallChunks = new ArrayList<>();
        String[] lines = content.split("\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (current.length() + line.length() > MAX_CHUNK_SIZE && current.length() > 0) {
                smallChunks.add(new CodeChunk(filePath, current.toString()));
                current = new StringBuilder();
            }
            current.append(line).append("\n");
        }

        if (current.length() > 0) {
            smallChunks.add(new CodeChunk(filePath, current.toString()));
        }

        return smallChunks;
    }
}