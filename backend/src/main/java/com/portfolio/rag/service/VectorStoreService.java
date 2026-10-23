package com.portfolio.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.rag.entity.DocumentChunk;
import com.portfolio.rag.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);
    private static final int LOCAL_VECTOR_DIM = 128;

    private final DocumentChunkRepository chunkRepository;
    private final PrivacyGuardService privacyGuardService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.embedding-model}")
    private String embeddingModel;

    public VectorStoreService(DocumentChunkRepository chunkRepository,
                              PrivacyGuardService privacyGuardService,
                              RestClient restClient,
                              ObjectMapper objectMapper) {
        this.chunkRepository = chunkRepository;
        this.privacyGuardService = privacyGuardService;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public double[] generateEmbedding(String text) {
        if ("demo-mode".equals(apiKey) || apiKey.isBlank()) {
            return computeLocalDeterministicEmbedding(text);
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + embeddingModel + ":embedContent?key=" + apiKey;
            Map<String, Object> body = Map.of(
                    "model", "models/" + embeddingModel,
                    "content", Map.of("parts", List.of(Map.of("text", text)))
            );
            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode values = objectMapper.readTree(response).path("embedding").path("values");
            double[] vector = new double[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).asDouble();
            }
            return vector;
        } catch (Exception e) {
            log.warn("Falling back to local deterministic embedding: {}", e.getMessage());
            return computeLocalDeterministicEmbedding(text);
        }
    }

    public List<ScoredChunk> searchSimilarChunks(String query, int topK) {
        double[] queryVector = generateEmbedding(query);
        Set<String> queryTokens = tokenize(query);

        List<DocumentChunk> allChunks = chunkRepository.findAll();
        List<ScoredChunk> scored = new ArrayList<>();

        for (DocumentChunk chunk : allChunks) {
            double[] chunkVector = deserializeVector(chunk.getSerializedEmbedding());
            String decryptedText = privacyGuardService.decrypt(chunk.getEncryptedContent());

            double cosine = cosineSimilarity(queryVector, chunkVector);
            double keywordOverlap = computeKeywordBoost(queryTokens, tokenize(decryptedText));
            double hybridScore = Math.min(0.99, (cosine * 0.7) + (keywordOverlap * 0.3));

            scored.add(new ScoredChunk(
                    chunk.getDocument().getTitle(),
                    chunk.getDocument().getDocType(),
                    chunk.getChunkIndex(),
                    decryptedText,
                    Math.round(hybridScore * 1000.0) / 1000.0
            ));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    public double cosineSimilarity(double[] v1, double[] v2) {
        int len = Math.min(v1.length, v2.length);
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < len; i++) {
            dot += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public String serializeVector(double[] vector) {
        return Arrays.stream(vector)
                .mapToObj(d -> String.format(Locale.US, "%.5f", d))
                .collect(Collectors.joining(","));
    }

    public double[] deserializeVector(String str) {
        if (str == null || str.isBlank()) return new double[LOCAL_VECTOR_DIM];
        String[] parts = str.split(",");
        double[] vec = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vec[i] = Double.parseDouble(parts[i]);
        }
        return vec;
    }

    private double[] computeLocalDeterministicEmbedding(String text) {
        double[] vec = new double[LOCAL_VECTOR_DIM];
        Set<String> tokens = tokenize(text);
        for (String token : tokens) {
            int hash = Math.abs(token.hashCode());
            vec[hash % LOCAL_VECTOR_DIM] += 1.0;
            vec[(hash / 7) % LOCAL_VECTOR_DIM] += 0.5;
        }
        return vec;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(w -> w.length() > 2)
                .collect(Collectors.toSet());
    }

    private double computeKeywordBoost(Set<String> qTokens, Set<String> docTokens) {
        if (qTokens.isEmpty()) return 0.0;
        long matches = qTokens.stream().filter(docTokens::contains).count();
        return (double) matches / qTokens.size();
    }

    public record ScoredChunk(
            String documentTitle,
            String docType,
            int chunkIndex,
            String content,
            double score
    ) {}
}
