package com.likith.AI.Code.Intelligence.SaaS.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    @Value("${hf.token}")
    private String HF_TOKEN;

    private static final String HF_URL =
            "https://router.huggingface.co/hf-inference/models/sentence-transformers/all-MiniLM-L6-v2/pipeline/feature-extraction";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> getEmbeddings(List<String> texts) {

        try {

            String requestBody = objectMapper.writeValueAsString(
                    Map.of("inputs", texts)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HF_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + HF_TOKEN)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            System.out.println("HF RESPONSE: " + body);

            if (body.startsWith("{")) {
                throw new RuntimeException("HF returned error: " + body);
            }

            List<List<Double>> embeddings = objectMapper.readValue(
                    body,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class,
                                    objectMapper.getTypeFactory()
                                            .constructCollectionType(List.class, Double.class))
            );

            return embeddings.stream()
                    .map(this::toVectorString)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Embedding failed: " + e.getMessage(), e);
        }
    }

    public String getEmbedding(String text) {
        return getEmbeddings(List.of(text)).get(0);
    }

    private String toVectorString(List<Double> embedding) {

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < embedding.size(); i++) {
            sb.append(embedding.get(i));
            if (i < embedding.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }
}