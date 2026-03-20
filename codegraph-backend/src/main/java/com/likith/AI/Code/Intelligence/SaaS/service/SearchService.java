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

    // ─── Step 1: Classify the question ───────────────────────────────────────────

    private String classifyQuestion(String question, String previousQuestion) {

        String q = question.toLowerCase().trim();

        // ── Rule 1: Follow-up with pronoun → always CODE ──────────────────────────
        if (previousQuestion != null && !previousQuestion.isBlank()) {
            if (q.startsWith("how is it") || q.startsWith("how it")
                    || q.startsWith("where is it") || q.startsWith("where it")
                    || q.startsWith("how does it") || q.startsWith("how do i")
                    || q.startsWith("how is this") || q.startsWith("how does this")
                    || q.startsWith("show me") || q.startsWith("where is this")
                    || q.contains("used in this") || q.contains("used here")
                    || q.contains("implemented here") || q.contains("implemented in this")
                    || q.contains("in this project") || q.contains("in this codebase")
                    || (q.contains("it") && q.contains("project"))
                    || (q.contains("this") && q.contains("project"))) {
                return "CODE";
            }
        }

        // ── Rule 2: "How does/is/are/do" → always CODE ───────────────────────────
        // Any question asking how something works belongs to the codebase
        if (q.startsWith("how does") || q.startsWith("how do ")
                || q.startsWith("how is") || q.startsWith("how are")
                || q.startsWith("how was") || q.startsWith("how can")
                || q.startsWith("explain how") || q.startsWith("describe how")
                || q.contains("how does the") || q.contains("how is the")
                || q.contains("how are the")) {
            return "CODE";
        }

        // ── Rule 3: "Where is/are" → CODE ────────────────────────────────────────
        if (q.startsWith("where is") || q.startsWith("where are")
                || q.startsWith("where does") || q.startsWith("where do")) {
            return "CODE";
        }

        // ── Rule 4: Folder/file listing → ARCHITECTURE ───────────────────────────
        if (q.contains("what files are in") || q.contains("list all files")
                || q.contains("files inside") || q.contains("what is inside the")
                || q.contains("list files") || q.contains("what files")
                || q.contains("show all files") || q.contains("files in the")) {
            return "ARCHITECTURE";
        }

        // ── Rule 5: Project-level questions → ARCHITECTURE ───────────────────────
        if (q.contains("what does this project") || q.contains("tell me about this project")
                || q.contains("what is this project") || q.contains("about the project")
                || q.contains("what frameworks") || q.contains("what technologies")
                || q.contains("tech stack") || q.contains("what libraries")) {
            return "ARCHITECTURE";
        }

        // ── Rule 6: Specific file names → FILE ───────────────────────────────────
        String[] fileKeywords = {".tsx", ".ts", ".jsx", ".js", ".java", ".xml",
                ".json", ".yml", ".yaml", ".md", ".properties",
                "pom.xml", "package.json", "dockerfile",
                "vite.config", "tailwind.config", "application.properties",
                "docker-compose", "readme"};
        for (String kw : fileKeywords) {
            if (q.contains(kw)) return "FILE";
        }

        // ── Rule 7: Specific class names → CLASS ─────────────────────────────────
        String[] words = question.split("\\s+");
        for (String word : words) {
            String clean = word.replaceAll("[^a-zA-Z0-9]", "");
            if (clean.length() >= 4 && Character.isUpperCase(clean.charAt(0))
                    && (clean.endsWith("Service") || clean.endsWith("Controller")
                    || clean.endsWith("Repository") || clean.endsWith("Component")
                    || clean.endsWith("Entity") || clean.endsWith("Config")
                    || clean.endsWith("Filter") || clean.endsWith("Handler"))) {
                return "CLASS";
            }
        }

        // ── Rule 8: Casual greetings → CASUAL ────────────────────────────────────
        if (q.equals("hi") || q.equals("hello") || q.equals("hey")
                || q.equals("thanks") || q.equals("thank you") || q.equals("ok")
                || q.equals("okay") || q.equals("good") || q.equals("great")
                || q.startsWith("hi ") || q.startsWith("hello ") || q.startsWith("hey ")) {
            return "CASUAL";
        }

        // ── Rule 9: LLM fallback for ambiguous questions ──────────────────────────
        String context = previousQuestion != null && !previousQuestion.isBlank()
                ? "Previous question: \"" + previousQuestion + "\"\n"
                : "";

        String prompt = """
You are a classifier for a code intelligence assistant called CodeGraph AI.
Users are asking questions about a specific software project's codebase.

%sClassify the following question into EXACTLY one of these categories:

CASUAL     - greetings, general chat, praise, thanks — ONLY if completely unrelated to any code or project
ARCHITECTURE - asking about tech stack, project structure, what the project does, how it works overall
FILE       - asking about a specific file by name
CLASS      - asking about a specific class or component by exact name
CODE       - anything else about how the project works, implements something, or handles something

Current question: "%s"

Reply with ONLY the single category word. No explanation.
""".formatted(context, question);

        String result = chatService.generateAnswer(prompt).strip().toUpperCase();

        if (result.contains("CASUAL")) return "CASUAL";
        if (result.contains("ARCHITECTURE")) return "ARCHITECTURE";
        if (result.contains("FILE")) return "FILE";
        if (result.contains("CLASS")) return "CLASS";
        return "CODE"; // default to CODE — better to search than answer generically
    }

    // ─── Step 2: Route to handler ─────────────────────────────────────────────────

    public AskResponse askQuestion(Long projectId, String question, String previousQuestion) {
        String category = classifyQuestion(question, previousQuestion);

        return switch (category) {
            case "CASUAL"       -> handleCasual(question);
            case "ARCHITECTURE" -> handleArchitecture(projectId, question);
            case "FILE"         -> handleFile(projectId, question);
            case "CLASS"        -> handleClass(projectId, question);
            default             -> handleCode(projectId, question, previousQuestion);
        };
    }

    // ─── CASUAL ───────────────────────────────────────────────────────────────────

    private AskResponse handleCasual(String question) {
        String prompt = """
You are CodeGraph AI — an AI assistant that helps developers understand codebases.
Respond to this message naturally and briefly. Do not mention code unless the user asks about it.

Message: %s
""".formatted(question);
        return new AskResponse(chatService.generateAnswer(prompt), false);
    }

    // ─── ARCHITECTURE ─────────────────────────────────────────────────────────────

    private AskResponse handleArchitecture(Long projectId, String question) {
        String q = question.toLowerCase();

        List<CodeChunkEntity> chunks = new ArrayList<>();

        if (q.contains("frontend") || q.contains("react") || q.contains("ui") || q.contains("client")) {
            chunks.addAll(repository.findByFileName(projectId, "package.json"));
            chunks.addAll(repository.findByFileName(projectId, "vite.config"));
            chunks.addAll(repository.findByFileName(projectId, "tailwind"));
            chunks.addAll(repository.findByFileName(projectId, "tsconfig"));
            chunks.addAll(repository.findByFileName(projectId, "codegraph-frontend"));
        } else if (q.contains("backend") || q.contains("java") || q.contains("spring") || q.contains("server")) {
            chunks.addAll(repository.findByFileName(projectId, "pom.xml"));
            chunks.addAll(repository.findByFileName(projectId, "application.properties"));
            chunks.addAll(repository.findByFileName(projectId, "Dockerfile"));
            chunks.addAll(repository.findByFileName(projectId, "codegraph-backend"));
        } else {
            chunks.addAll(repository.findByFileName(projectId, "pom.xml"));
            chunks.addAll(repository.findByFileName(projectId, "package.json"));
            chunks.addAll(repository.findByFileName(projectId, "application.properties"));
            chunks.addAll(repository.findByFileName(projectId, "README"));
            chunks.addAll(repository.findByFileName(projectId, "Dockerfile"));
        }

        if (chunks.isEmpty()) return handleCode(projectId, question, "");

        String context = chunks.stream()
                .limit(6)
                .map(c -> "FILE: " + c.getFilePath() + "\nCONTENT:\n" + c.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
You are CodeGraph AI — an expert software engineer explaining a codebase to a developer.

Using the project files provided below, give a thorough and insightful answer.

Your response MUST cover:
1. What problem this project solves and why it exists
2. How the system works end-to-end (the full flow from user action to result)
3. The key technologies used and WHY they were chosen (not just a list)
4. How the frontend and backend connect
5. The most important components and what each one does

Guidelines:
- Write in clear paragraphs, not just bullet points
- Be specific — mention actual class names, file names, and technologies from the context
- Explain the "why" behind technical decisions where you can infer it
- Keep it informative but not overly long

Project Files:
%s

User Question:
%s
""".formatted(context, question);

        return new AskResponse(chatService.generateAnswer(prompt), false);
    }

    // ─── FILE ─────────────────────────────────────────────────────────────────────

    private AskResponse handleFile(Long projectId, String question) {
        String fileName = extractFileName(question);

        List<CodeChunkEntity> chunks = fileName.isEmpty()
                ? List.of()
                : repository.findByFileName(projectId, fileName);

        if (chunks.isEmpty()) return handleCode(projectId, question, "");

        String context = chunks.stream()
                .limit(5)
                .map(c -> "FILE: " + c.getFilePath() + "\nCONTENT:\n" + c.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
You are CodeGraph AI — an expert software engineer.

You have been given the contents of a specific file from the project.

Explain clearly:
1. What this file is responsible for in one sentence
2. The key classes, functions, configurations, or logic inside it
3. How this file connects to or is used by the rest of the project
4. Any important patterns, annotations, or design decisions visible in the code

Be specific and technical. Reference actual code from the context.

File Contents:
%s

User Question:
%s
""".formatted(context, question);

        return new AskResponse(chatService.generateAnswer(prompt), true);
    }

    // ─── CLASS ────────────────────────────────────────────────────────────────────

    private AskResponse handleClass(Long projectId, String question) {
        String className = extractClassName(question);

        List<CodeChunkEntity> chunks = className == null
                ? List.of()
                : repository.findByFileName(projectId, className);

        if (chunks.isEmpty()) return handleCode(projectId, question, "");

        String context = chunks.stream()
                .limit(5)
                .map(c -> "FILE: " + c.getFilePath() + "\nCODE:\n" + c.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
You are CodeGraph AI — a senior software engineer explaining a specific class or component.

You have been given source code from a specific class or component.

Explain clearly:
1. What this class/component is responsible for
2. Every important method or function — what it does and how it works
3. Key fields, dependencies, and annotations
4. How this class interacts with other parts of the system
5. The execution flow — trace what happens step by step when the main method is called

Be specific. Reference actual method names and logic from the code.

Source Code:
%s

User Question:
%s
""".formatted(context, question);

        return new AskResponse(chatService.generateAnswer(prompt), true);
    }

    // ─── CODE ─────────────────────────────────────────────────────────────────────

    private AskResponse handleCode(Long projectId, String question, String previousQuestion) {
        // Enrich query with previous question context for better vector search
        String enrichedQuery = (previousQuestion != null && !previousQuestion.isBlank())
                ? previousQuestion + " " + question
                : question;

        String queryEmbedding = embeddingService.getEmbedding(enrichedQuery);
        String q = question.toLowerCase();

        List<CodeChunkEntity> chunks;

        if (q.contains("frontend") || q.contains("react") || q.contains("component") || q.contains("ui")) {
            chunks = repository.findSimilarChunksInPath(queryEmbedding, projectId, "codegraph-frontend", 5);
            if (chunks.isEmpty()) chunks = repository.searchSimilarChunks(projectId, queryEmbedding, 5);
        } else if (q.contains("backend") || q.contains("spring") || q.contains("java") || q.contains("api")) {
            chunks = repository.findSimilarChunksInPath(queryEmbedding, projectId, "codegraph-backend", 5);
            if (chunks.isEmpty()) chunks = repository.searchSimilarChunks(projectId, queryEmbedding, 5);
        } else {
            chunks = repository.searchSimilarChunks(projectId, queryEmbedding, 5);
        }

        List<CodeChunkEntity> relevant = chunks.stream()
                .filter(c -> c.getContent() != null && c.getContent().trim().length() > 100)
                .filter(c -> c.getSimilarity() != null && c.getSimilarity() < 0.8)
                .limit(3)
                .collect(Collectors.toList());

        if (relevant.isEmpty()) {
            String directPrompt = """
You are CodeGraph AI — a helpful assistant for developers.
Answer this question as best you can. If it requires specific code knowledge you don't have, say so honestly.

Question: %s
""".formatted(question);
            return new AskResponse(chatService.generateAnswer(directPrompt), false);
        }

        String context = relevant.stream()
                .map(c -> "FILE: " + c.getFilePath() + "\nCODE:\n" + c.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        String questionContext = (previousQuestion != null && !previousQuestion.isBlank())
                ? "Note: The user previously asked about \"" + previousQuestion + "\". Answer in that context.\n\n"
                : "";

        String prompt = """
You are CodeGraph AI — a senior software engineer helping a developer understand a codebase.

You have been given code snippets retrieved from the project that are relevant to the question.

%sIMPORTANT RULES:
- Answer using ONLY the code context provided below
- If the answer is not present in the context, say "I couldn't find information about this in the indexed code"
- Do NOT invent code, methods, or logic that is not in the context
- Reference specific file names, class names, and method names from the context

Answer structure:
1. Direct answer to the question in 1-2 sentences
2. Detailed explanation referencing specific code from the context
3. Mention which files/classes are involved

Code Context:
%s

User Question:
%s
""".formatted(questionContext, context, question);

        return new AskResponse(chatService.generateAnswer(prompt), true);
    }

    // ─── Search endpoint ──────────────────────────────────────────────────────────

    public List<SearchResult> search(Long projectId, String query) {
        String queryEmbedding = embeddingService.getEmbedding(query);
        List<CodeChunkEntity> chunks = repository.searchSimilarChunks(projectId, queryEmbedding, 10);

        if (chunks.isEmpty()) return new ArrayList<>();

        return chunks.stream()
                .filter(c -> c.getContent() != null && c.getContent().trim().length() > 100)
                .filter(c -> c.getSimilarity() != null && c.getSimilarity() < 0.8)
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

        if (q.contains("application.properties") || q.contains("application properties")) return "application.properties";
        if (q.contains("docker-compose") || q.contains("docker compose")) return "docker-compose.yml";
        if (q.contains("pom.xml") || q.contains("pom xml")) return "pom.xml";
        if (q.contains("package.json")) return "package.json";
        if (q.contains("vite.config")) return "vite.config.ts";
        if (q.contains("tailwind.config")) return "tailwind.config.js";
        if (q.contains("dockerfile")) return "Dockerfile";
        if (q.contains("readme")) return "README.md";

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
        String[] words = question.split("\\s+");
        for (String word : words) {
            String clean = word.replaceAll("[^a-zA-Z0-9]", "");
            if (clean.length() >= 4
                    && Character.isUpperCase(clean.charAt(0))
                    && (clean.endsWith("Service") || clean.endsWith("Controller")
                    || clean.endsWith("Repository") || clean.endsWith("Component")
                    || clean.endsWith("Entity") || clean.endsWith("Config")
                    || clean.endsWith("Filter") || clean.endsWith("Handler")
                    || clean.endsWith("Interface") || clean.endsWith("Store"))) {
                return clean;
            }
        }
        return null;
    }
}