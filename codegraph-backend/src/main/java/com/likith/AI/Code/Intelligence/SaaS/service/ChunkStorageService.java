package com.likith.AI.Code.Intelligence.SaaS.service;

import com.likith.AI.Code.Intelligence.SaaS.model.CodeChunk;
import com.likith.AI.Code.Intelligence.SaaS.repository.CodeChunkRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkStorageService {

    private final EmbeddingService embeddingService;
    private final CodeChunkRepository codeChunkRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ChunkStorageService(EmbeddingService embeddingService,
                               CodeChunkRepository codeChunkRepository) {
        this.embeddingService = embeddingService;
        this.codeChunkRepository = codeChunkRepository;
    }

    @Transactional
    public void deleteByProjectId(Long projectId) {
        codeChunkRepository.deleteByProjectId(projectId);
    }

    @Transactional
    public void storeChunks(Long projectId, List<CodeChunk> chunks) {

        int batchSize = 10;

        for (int i = 0; i < chunks.size(); i += batchSize) {

            List<CodeChunk> batch =
                    chunks.subList(i, Math.min(i + batchSize, chunks.size()));

            List<String> texts = new ArrayList<>();

            for (CodeChunk chunk : batch) {
                texts.add(chunk.getChunkContent());
            }

            List<String> embeddings = embeddingService.getEmbeddings(texts);

            for (int j = 0; j < batch.size(); j++) {

                CodeChunk chunk = batch.get(j);
                String embedding = embeddings.get(j);

                entityManager.createNativeQuery("""
                        INSERT INTO code_chunks (project_id, file_path, content, embedding)
                        VALUES (:projectId, :filePath, :content, (:embedding)::vector)
                        """)
                        .setParameter("projectId", projectId)
                        .setParameter("filePath", chunk.getFilePath())
                        .setParameter("content", chunk.getChunkContent())
                        .setParameter("embedding", embedding)
                        .executeUpdate();
            }
        }
    }
}