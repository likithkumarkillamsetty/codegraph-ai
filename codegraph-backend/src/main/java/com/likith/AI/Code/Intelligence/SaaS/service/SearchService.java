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

    // ─── Step 1: Ask LLM to classify the question ───────────────────────────────

    private String classifyQuestion(String question) {
        String prompt = """
You are a classifier for a code intelligence assistant.
Classify the following question into EXACTLY one of these categories:

CASUAL     - greetings, general chat, non-technical ("hi", "thanks", "what is AI")
ARCHITECTURE - asking about tech stack, frameworks, dependencies, project structure, frontend/backend technologies
FILE       - asking about a specific file by name (e.g. "show me App.tsx", "what's in pom.xml")
CLASS      - asking about a specific Java class or React component by name (e.g. "how does SearchService work")
CODE       - asking about specific functionality, logic, flow, or how something works in the codebase

Question: "%s"

Reply with ONLY the single category word. No explanation. No punctuation.
""".formatted(question);

        String result = chatService.generateAnswer(prompt).strip().toUpperCase();

        // Sanitize — only accept known categories
        if (result.contains("CASUAL")) return "CASUAL";
        if (result.contains("ARCHITECTURE")) return "ARCHITECTURE";
        if (result.contains("FILE")) return "FILE";
        if (result.contains("CLASS")) return "CLASS";
        if (result.contains("CODE")) return "CODE";
        return "CODE"; // safe fallback
    }

    // ─── Step 2: Route to correct handler ────────────────────────────────────────

    public AskResponse askQuestion(Long projectId, String question) {

        String category = classifyQuestion(question);

        return switch (category) {
            case "CASUAL"       -> handleCasual(question);
            case "ARCHITECTURE" -> handleArchitecture(projectId, question);
            case "FILE"         -> handleFile(projectId, question);
            case "CLASS"        -> handleClass(projectId, question);
            default             -> handleCode(projectId, question);
        };
    }

    // ─── CASUAL: No RAG, direct answer ───────────────────────────────────────────

    private AskResponse handleCasual(String question) {
        String prompt = """
You are CodeGraph AI, a helpful assistant for developers.
Answer this conversational message naturally and briefly.

Message: %s
""".formatted(question);
        return new AskResponse(chatService.generateAnswer(prompt), false);
    }

    // ─── ARCHITECTURE: Fetch config + package files ───────────────────────────────

    private AskResponse handleArchitecture(Long projectId, String question) {
        String q = question.toLowerCase();

        List<CodeChunkEntity> chunks = new ArrayList<>();

        // If question is specifically about frontend, bias toward frontend files
        if (q.contains("frontend") || q.contains("react") || q.contains("ui") || q.contains("client")) {
            chunks.addAll(repository.findByFileName(projectId, "package.json"));
            chunks.addAll(repository.findByFileName(projectId, "vite.config"));
            chunks.addAll(repository.findByFileName(projectId, "tailwind"));
            chunks.addAll(repository.findByFileName(projectId, "tsconfig"));
            chunks.addAll(repository.findByFilePathContaining(projectId, "codegraph-frontend", 5));
        }
        // If specifically about backend
        else if (q.contains("backend") || q.contains("java") || q.contains("spring") || q.contains("server")) {
            chunks.addAll(repository.findByFileName(projectId, "pom.xml"));
            chunks.addAll(repository.findByFileName(projectId, "application.properties"));
            chunks.addAll(repository.findByFileName(projectId, "Dockerfile"));
            chunks.addAll(repository.findByFilePathContaining(projectId, "codegraph-backend", 5));
        }
        // General architecture question — fetch both sides
        else {
            chunks.addAll(repository.findByFileName(projectId, "pom.xml"));
            chunks.addAll(repository.findByFileName(projectId, "package.json"));
            chunks.addAll(repository.findByFileName(projectId, "application.properties"));
            chunks.addAll(repository.findByFileName(projectId, "readme"));
            chunks.addAll(repository.findByFileName(projectId, "Dockerfile"));
        }

        if (chunks.isEmpty()) {
            // Fallback to vector search if no config files found
            return handleCode(projectId, question);
        }

        String context = chunks.stream()
                .limit(6)
                .map(c -> "FILE: " + c.getFilePath() + "\nCONTENT:\n" + c.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
You are CodeGraph AI — an expert software engineer explaining a codebase.

You have been given configuration and structure files from the project.

Answer the user's question clearly:
- List the technologies, frameworks, and libraries used
- Mention which files confirm this (package.json, pom.xml, etc.)
- Separate frontend and backend concerns if relevant
- Be concise and accurate

Code Context:
%s

User Question:
%s
""".formatted(context, question);

        return new AskResponse(chatService.generateAnswer(prompt), true);
    }

    // ─── FILE: Fetch specific file by name ───────────────────────────────────────

    private AskResponse handleFile(Long projectId, String question) {
        // Extract filename from question
        String fileName = extractFileName(question);

        List<CodeChunkEntity> chunks = fileName.isEmpty()
                ? List.of()
                : repository.findByFileName(projectId, fileName);

        if (chunks.isEmpty()) {
            // Fallback to vector search
            return handleCode(projectId, question);
        }

        String context = chunks.stream()
                .limit(5)
                .map(c -> "FILE: " + c.getFilePath() + "\nCONTENT:\n" + c.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
You are CodeGraph AI — an expert software engineer.

You have been given the contents of a specific file from the project.

Explain:
- What this file is responsible for
- Key configurations, classes, or logic inside it
- How it relates to the rest of the project

Code Context:
%s

User Question:
%s
""".formatted(context, question);

        return new AskResponse(chatService.generateAnswer(prompt), true);
    }

    // ─── CLASS: Fetch specific class by name ─────────────────────────────────────

    private AskResponse handleClass(Long projectId, String question) {
        String className = extractClassName(question);

        List<CodeChunkEntity> chunks = className == null
                ? List.of()
                : repository.findByFileName(projectId, className);

        if (chunks.isEmpty()) {
            return handleCode(projectId, question);
        }

        String context = chunks.stream()
                .limit(5)
                .map(c -> "FILE: " + c.getFilePath() + "\nCODE:\n" + c.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
You are CodeGraph AI — a senior software engineer explaining a codebase.

You have been given source code from a specific class or component.

Explain:
- What this class/component is responsible for
- Important methods, fields, and their purpose
- How it interacts with other parts of the system
- Frameworks or patterns used

Code Context:
%s

User Question:
%s
""".formatted(context, question);

        return new AskResponse(chatService.generateAnswer(prompt), true);
    }

    // ─── CODE: Vector similarity search + RAG ────────────────────────────────────

    private AskResponse handleCode(Long projectId, String question) {
        String queryEmbedding = embeddingService.getEmbedding(question);

        // Path-aware search: bias results toward frontend or backend if mentioned
        List<CodeChunkEntity> chunks;
        String q = question.toLowerCase();

        if (q.contains("frontend") || q.contains("react") || q.contains("component") || q.contains("ui")) {
            chunks = repository.findSimilarChunksInPath(queryEmbedding, projectId, "codegraph-frontend", 5);
            if (chunks.isEmpty()) {
                chunks = repository.searchSimilarChunks(projectId, queryEmbedding, 5);
            }
        } else if (q.contains("backend") || q.contains("spring") || q.contains("java") || q.contains("api")) {
            chunks = repository.findSimilarChunksInPath(queryEmbedding, projectId, "codegraph-backend", 5);
            if (chunks.isEmpty()) {
                chunks = repository.searchSimilarChunks(projectId, queryEmbedding, 5);
            }
        } else {
            chunks = repository.searchSimilarChunks(projectId, queryEmbedding, 5);
        }

        if (chunks.isEmpty()) {
            return new AskResponse(chatService.generateAnswer(question), false);
        }

        // Filter out low-quality chunks (too short or low similarity)
        chunks = chunks.stream()
                .filter(c -> c.getContent() != null && c.getContent().trim().length() > 50)
                .filter(c -> c.getSimilarity() != null && c.getSimilarity() > 0.2)
                .limit(3)
                .collect(Collectors.toList());

        if (chunks.isEmpty()) {
            return handleCasual(question);
        }

        String context = chunks.stream()
                .map(c -> "FILE: " + c.getFilePath() + "\nCODE:\n" + c.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
You are CodeGraph AI — a senior software engineer helping developers understand a codebase.

You have been given relevant code snippets retrieved from the project.

Instructions:
1. Answer the question directly and concisely first.
2. Reference specific classes, methods, and files from the context.
3. Explain the logic step-by-step where relevant.
4. Do NOT invent code or logic not present in the context.

Code Context:
%s

User Question:
%s
""".formatted(context, question);

        return new AskResponse(chatService.generateAnswer(prompt), true);
    }

    // ─── Search endpoint (used by /search API) ────────────────────────────────────

    public List<SearchResult> search(Long projectId, String query) {
        String queryEmbedding = embeddingService.getEmbedding(query);
        List<CodeChunkEntity> chunks = repository.searchSimilarChunks(projectId, queryEmbedding, 10);

        if (chunks.isEmpty()) return new ArrayList<>();

        return chunks.stream()
                .filter(c -> c.getContent() != null && c.getContent().trim().length() > 50)
                .limit(3)
                .map(chunk -> new SearchResult(
                        chunk.getFilePath(),
                        chunk.getContent(),
                        chunk.getSimilarity()
                ))
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private String extractFileName(String question) {
        String q = question.toLowerCase();

        // Named files
        if (q.contains("application.properties") || q.contains("application properties")) return "application.properties";
        if (q.contains("docker-compose") || q.contains("docker compose")) return "docker-compose.yml";
        if (q.contains("pom.xml") || q.contains("pom xml")) return "pom.xml";
        if (q.contains("package.json")) return "package.json";
        if (q.contains("vite.config")) return "vite.config.ts";
        if (q.contains("tailwind.config")) return "tailwind.config.js";
        if (q.contains("dockerfile")) return "Dockerfile";
        if (q.contains("readme")) return "README.md";

        // Extract by extension
        String[] extensions = {".tsx", ".ts", ".jsx", ".js", ".java", ".properties",
                ".yml", ".yaml", ".xml", ".json", ".sql", ".md", ".txt"};
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

    private String extractClassName(String question) {
        // Only match actual Java class names or React component names (PascalCase, 4+ chars, ends with known suffix)
        String[] words = question.split("\\s+");
        for (String word : words) {
            String clean = word.replaceAll("[^a-zA-Z0-9]", "");
            if (clean.length() >= 4
                    && Character.isUpperCase(clean.charAt(0))
                    && (clean.endsWith("Service") || clean.endsWith("Controller")
                    || clean.endsWith("Repository") || clean.endsWith("Component")
                    || clean.endsWith("Entity") || clean.endsWith("Config")
                    || clean.endsWith("Filter") || clean.endsWith("Handler"))) {
                return clean;
            }
        }
        return null;
    }
}