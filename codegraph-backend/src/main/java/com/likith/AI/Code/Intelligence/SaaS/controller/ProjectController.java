package com.likith.AI.Code.Intelligence.SaaS.controller;

import com.likith.AI.Code.Intelligence.SaaS.dto.AskResponse;
import com.likith.AI.Code.Intelligence.SaaS.dto.ChatMessage;
import com.likith.AI.Code.Intelligence.SaaS.dto.CreateProjectRequest;
import com.likith.AI.Code.Intelligence.SaaS.dto.SearchResult;
import com.likith.AI.Code.Intelligence.SaaS.entity.Project;
import com.likith.AI.Code.Intelligence.SaaS.model.CodeChunk;
import com.likith.AI.Code.Intelligence.SaaS.model.SourceFile;
import com.likith.AI.Code.Intelligence.SaaS.service.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;
    private final FileScannerService fileScannerService;
    private final FileContentService fileContentService;
    private final CodeChunkService codeChunkService;
    private final ChunkStorageService chunkStorageService;
    private final SearchService searchService;

    public ProjectController(ProjectService service,
                             FileScannerService fileScannerService,
                             FileContentService fileContentService,
                             CodeChunkService codeChunkService,
                             ChunkStorageService chunkStorageService,
                             SearchService searchService) {
        this.service = service;
        this.fileScannerService = fileScannerService;
        this.fileContentService = fileContentService;
        this.codeChunkService = codeChunkService;
        this.chunkStorageService = chunkStorageService;
        this.searchService = searchService;
    }

    @PostMapping
    public Project createProject(@RequestBody CreateProjectRequest request) {
        return service.createProject(
                request.getName(),
                request.getGithubUrl()
        );
    }

    @GetMapping("/{id}/files")
    public List<String> getJavaFiles(@PathVariable Long id) {
        Project project = service.getProjectById(id);
        List<File> files = fileScannerService.scanJavaFiles(project.getLocalPath());
        return files.stream().map(File::getAbsolutePath).toList();
    }

    @GetMapping("/{id}/content")
    public List<SourceFile> getJavaFileContent(@PathVariable Long id) {
        Project project = service.getProjectById(id);
        List<File> files = fileScannerService.scanJavaFiles(project.getLocalPath());
        return fileContentService.readJavaFiles(files);
    }

    @GetMapping("/{id}/chunks")
    public List<CodeChunk> getChunks(@PathVariable Long id) {
        Project project = service.getProjectById(id);
        List<File> files = fileScannerService.scanJavaFiles(project.getLocalPath());
        List<SourceFile> sourceFiles = fileContentService.readJavaFiles(files);
        return codeChunkService.chunkSourceFiles(sourceFiles);
    }

    @PostMapping("/{id}/embed")
    public String embedProject(@PathVariable Long id) {
        // Delete stale chunks before re-embedding
        chunkStorageService.deleteByProjectId(id);

        Project project = service.getProjectById(id);
        List<File> files = fileScannerService.scanJavaFiles(project.getLocalPath());
        List<SourceFile> sourceFiles = fileContentService.readJavaFiles(files);
        List<CodeChunk> chunks = codeChunkService.chunkSourceFiles(sourceFiles);
        chunkStorageService.storeChunks(id, chunks);
        return "Embedding completed";
    }

    @PostMapping("/{id}/search")
    public List<SearchResult> search(
            @PathVariable Long id,
            @RequestBody String query) {
        return searchService.search(id, query);
    }

    @PostMapping("/{id}/ask")
    public AskResponse askProject(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");

        // Parse conversation history (new format) or fallback to previousQuestion (legacy)
        List<ChatMessage> history = new ArrayList<>();
        Object historyObj = body.get("history");
        if (historyObj instanceof List) {
            for (Object item : (List<?>) historyObj) {
                if (item instanceof Map) {
                    Map<?,?> map = (Map<?,?>) item;
                    String role = (String) map.get("role");
                    String content = (String) map.get("content");
                    if (role != null && content != null) {
                        history.add(new ChatMessage(role, content));
                    }
                }
            }
        }

        // Legacy support: previousQuestion as single string
        if (history.isEmpty() && body.containsKey("previousQuestion")) {
            String prev = (String) body.get("previousQuestion");
            if (prev != null && !prev.isBlank()) {
                history.add(new ChatMessage("user", prev));
            }
        }

        return searchService.askQuestion(id, question, history);
    }
}