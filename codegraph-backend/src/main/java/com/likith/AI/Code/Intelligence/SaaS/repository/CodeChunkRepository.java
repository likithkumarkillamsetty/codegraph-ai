package com.likith.AI.Code.Intelligence.SaaS.repository;

import com.likith.AI.Code.Intelligence.SaaS.entity.CodeChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CodeChunkRepository extends JpaRepository<CodeChunkEntity, Long> {

    @Query(value = """
        SELECT id, project_id, file_path, content,
               embedding <-> CAST(:embedding AS vector) AS similarity
        FROM code_chunks
        WHERE project_id = :projectId
          AND (embedding <-> CAST(:embedding AS vector)) < :threshold
        ORDER BY embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<CodeChunkEntity> searchSimilarChunks(
            @Param("projectId") Long projectId,
            @Param("embedding") String embedding,
            @Param("limit") int limit,
            @Param("threshold") double threshold
    );

    @Query(value = """
        SELECT *
        FROM code_chunks
        WHERE project_id = :projectId
        AND LOWER(file_path) LIKE LOWER(CONCAT('%', :fileName, '%'))
        LIMIT 5
        """, nativeQuery = true)
    List<CodeChunkEntity> findByFileName(
            @Param("projectId") Long projectId,
            @Param("fileName") String fileName
    );

    @Query(value = """
        SELECT id, project_id, file_path, content,
               embedding <-> CAST(:embedding AS vector) AS similarity
        FROM code_chunks
        WHERE project_id = :projectId
          AND LOWER(file_path) LIKE LOWER(CONCAT('%', :pathFragment, '%'))
          AND (embedding <-> CAST(:embedding AS vector)) < :threshold
        ORDER BY embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<CodeChunkEntity> findSimilarChunksInPath(
            @Param("embedding") String embedding,
            @Param("projectId") Long projectId,
            @Param("pathFragment") String pathFragment,
            @Param("limit") int limit,
            @Param("threshold") double threshold
    );

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM code_chunks WHERE project_id = :projectId", nativeQuery = true)
    void deleteByProjectId(@Param("projectId") Long projectId);
}