package com.likith.AI.Code.Intelligence.SaaS.service;

import com.likith.AI.Code.Intelligence.SaaS.dto.AskResponse;
import com.likith.AI.Code.Intelligence.SaaS.dto.SearchResult;
import com.likith.AI.Code.Intelligence.SaaS.entity.CodeChunkEntity;
import com.likith.AI.Code.Intelligence.SaaS.repository.CodeChunkRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final CodeChunkRepository repository;
    private final EmbeddingService embeddingService;
    private final OllamaChatService chatService;

    public SearchService(CodeChunkRepository repository,
                         EmbeddingService embeddingService,
                         OllamaChatService chatService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.chatService = chatService;
    }

    private boolean mentionsFile(String query) {
        String q = query.toLowerCase();
        return q.contains(".properties") || q.contains(".yml") || q.contains(".yaml") ||
                q.contains(".xml") || q.contains(".java") || q.contains(".json") ||
                q.contains(".sql") || q.contains(".md") || q.contains(".txt") ||
                q.contains("application properties") || q.contains("application.properties") ||
                q.contains("docker compose") || q.contains("docker-compose") ||
                q.contains("pom.xml") || q.contains("pom xml");
    }

    private String extractFileName(String query) {
        String q = query.toLowerCase();
        if (q.contains("application properties") || q.contains("application.properties"))
            return "application.properties";
        if (q.contains("docker compose") || q.contains("docker-compose"))
            return "docker-compose.yml";
        if (q.contains("pom.xml") || q.contains("pom xml"))
            return "pom.xml";

        String[] extensions = {".properties", ".yml", ".yaml", ".xml", ".java", ".json", ".sql", ".md", ".txt"};
        for (String ext : extensions) {
            int idx = q.indexOf(ext);
            if (idx != -1) {
                int start = idx;
                while (start > 0 && q.charAt(start - 1) != ' ') start--;
                return q.substring(start, idx + ext.length());
            }
        }
        return "";
    }

    public List<SearchResult> search(Long projectId, String query) {

        if (mentionsFile(query)) {
            String fileName = extractFileName(query);
            if (!fileName.isEmpty()) {
                List<CodeChunkEntity> fileChunks = repository.findByFileName(projectId, fileName);
                if (!fileChunks.isEmpty()) {
                    return fileChunks.stream()
                            .limit(1)
                            .map(chunk -> new SearchResult(
                                    chunk.getFilePath(),
                                    chunk.getContent(),
                                    chunk.getSimilarity()
                            ))
                            .toList();
                }
            }
        }

        String queryEmbedding = embeddingService.getEmbedding(query);
        List<CodeChunkEntity> chunks = repository.searchSimilarChunks(projectId, queryEmbedding, 10);

        if (chunks.isEmpty()) return new ArrayList<>();

        return chunks.stream()
                .limit(3)
                .map(chunk -> new SearchResult(
                        chunk.getFilePath(),
                        chunk.getContent(),
                        chunk.getSimilarity()
                ))
                .toList();
    }

    private String detectClassName(String question) {
        String[] words = question.split("\\s+");
        for (String w : words) {
            if (w.length() > 3 && Character.isUpperCase(w.charAt(0))) {
                return w.replaceAll("[^a-zA-Z0-9]", "");
            }
        }
        return null;
    }

    public AskResponse askQuestion(Long projectId, String question) {

        String q = question.toLowerCase().trim();

        // 1️⃣ Detect class / file name in question
        String className = detectClassName(question);

        if (className != null) {
            List<CodeChunkEntity> classChunks = repository.findByFileName(projectId, className);

            if (!classChunks.isEmpty()) {
                String context = classChunks.stream()
                        .limit(5)
                        .map(c -> "FILE: " + c.getFilePath() + "\nCODE:\n" + c.getContent())
                        .collect(Collectors.joining("\n\n"));

                String prompt = """
You are CodeGraph AI — an expert software engineer helping developers understand a codebase.

You have been given source code from a specific class/file.

Your task:
- Explain what this class/file is responsible for
- Describe important methods, fields, and responsibilities
- Explain how it interacts with the rest of the system
- Mention frameworks or patterns used (Spring Boot, JPA, etc.)

Guidelines:
- Use ONLY the provided code context
- Reference class names, methods, and files
- Keep explanations concise but technical

Code Context:
%s

User Question:
%s
""".formatted(context, question);

                String answer = chatService.generateAnswer(prompt);
                return new AskResponse(answer, true);
            }
        }

        // 2️⃣ Generate embedding for vector search
        String queryEmbedding = embeddingService.getEmbedding(question);
        List<CodeChunkEntity> chunks = repository.searchSimilarChunks(projectId, queryEmbedding, 10);

        if (chunks.isEmpty()) {
            String answer = chatService.generateAnswer(question);
            return new AskResponse(answer, false);
        }

        double similarity = chunks.get(0).getSimilarity();

        // 3️⃣ Project-level questions
        if (q.contains("project") || q.contains("repository")
                || q.contains("repo") || q.contains("codebase")
                || q.contains("system") || q.contains("application")) {

            List<CodeChunkEntity> projectChunks = new ArrayList<>();
            projectChunks.addAll(repository.findByFileName(projectId, "pom.xml"));
            projectChunks.addAll(repository.findByFileName(projectId, "application"));
            projectChunks.addAll(repository.findByFileName(projectId, "docker"));
            projectChunks.addAll(repository.findByFileName(projectId, "readme"));
            projectChunks.addAll(chunks);

            String context = projectChunks.stream()
                    .limit(6)
                    .map(c -> "FILE: " + c.getFilePath() + "\nCODE:\n" + c.getContent())
                    .collect(Collectors.joining("\n\n"));

            String prompt = """
You are CodeGraph AI — an AI assistant designed to explain software repositories.

You are given code snippets from a project.

Explain the project clearly.

Focus on:
- What problem this project solves
- Main technologies used
- Important components and services
- How the system works at a high level
- Key controllers, services, and data flow

Guidelines:
- Base your answer ONLY on the provided code context
- Mention file names and classes when relevant
- Use headings or bullet points

Code Context:
%s

User Question:
%s
""".formatted(context, question);

            String answer = chatService.generateAnswer(prompt);
            return new AskResponse(answer, true);
        }

        // 4️⃣ Stop RAG for unrelated chat
        if (similarity < 0.3) {
            String answer = chatService.generateAnswer(question);
            return new AskResponse(answer, false);
        }

        // 5️⃣ Normal RAG pipeline
        String context = chunks.stream()
                .limit(3)
                .map(c -> "FILE: " + c.getFilePath() + "\nCODE:\n" + c.getContent())
                .collect(Collectors.joining("\n\n"));

        String prompt = """
You are CodeGraph AI — a senior software engineer helping developers understand code.

You are given relevant code snippets from a real project.

Instructions:
1. Answer the question briefly first.
2. Then explain the relevant code in detail.
3. Mention specific classes, methods, and files involved.
4. Describe how the logic works step-by-step.

Rules:
- Use ONLY the provided code context
- Do not invent code
- Keep explanations precise and technical

Code Context:
%s

User Question:
%s
""".formatted(context, question);

        String answer = chatService.generateAnswer(prompt);
        return new AskResponse(answer, true);
    }
}