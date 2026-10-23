package com.portfolio.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.rag.dto.ChatQueryRequest;
import com.portfolio.rag.dto.ChatResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);

    private final VectorStoreService vectorStoreService;
    private final PrivacyGuardService privacyGuardService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.chat-model}")
    private String geminiModel;

    @Value("${app.ollama.base-url}")
    private String ollamaUrl;

    @Value("${app.ollama.model}")
    private String ollamaModel;

    public RagChatService(VectorStoreService vectorStoreService,
                          PrivacyGuardService privacyGuardService,
                          RestClient restClient,
                          ObjectMapper objectMapper) {
        this.vectorStoreService = vectorStoreService;
        this.privacyGuardService = privacyGuardService;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public ChatResponseDto askQuestion(ChatQueryRequest request) {
        int topK = request.topK() > 0 ? request.topK() : 3;
        String sanitizedQuestion = request.scrubPii()
                ? privacyGuardService.redactSensitiveData(request.question()).sanitizedText()
                : request.question();

        List<VectorStoreService.ScoredChunk> retrieved = vectorStoreService.searchSimilarChunks(sanitizedQuestion, topK);

        String contextBlock = retrieved.stream()
                .map(c -> String.format("\n%s",
                        c.documentTitle(), c.chunkIndex(), c.score(), c.content()))
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = """
                You are VaultMind AI, a privacy-preserving Personal Knowledge Assistant.
                Answer the user's question accurately using ONLY the provided knowledge base context chunks below.
                Always cite your sources inline using the format.
                If the answer is not in the context, state clearly that your personal knowledge base does not contain that information.

                === RETRIEVED KNOWLEDGE BASE CHUNKS ===
                %s

                === USER QUESTION ===
                %s
                """.formatted(contextBlock, sanitizedQuestion);

        String provider = request.provider() != null ? request.provider().toUpperCase() : "GEMINI";
        String answer;

        if ("OLLAMA".equals(provider)) {
            answer = callOllamaOrFallback(systemPrompt, retrieved);
        } else {
            answer = callGeminiOrFallback(systemPrompt, retrieved);
        }

        List<ChatResponseDto.CitationDto> citations = retrieved.stream()
                .map(c -> new ChatResponseDto.CitationDto(
                        c.documentTitle(), c.docType(), c.chunkIndex(), c.score(), c.content()
                ))
                .toList();

        return new ChatResponseDto(answer, provider, request.scrubPii(), citations);
    }

    private String callGeminiOrFallback(String prompt, List<VectorStoreService.ScoredChunk> retrieved) {
        if ("demo-mode".equals(apiKey) || apiKey.isBlank()) {
            return buildSynthesizedAnswer("Gemini Demo Mode", retrieved);
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + apiKey;
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );
            String raw = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(raw);
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            log.warn("Gemini API call failed, returning synthesized RAG response: {}", e.getMessage());
            return buildSynthesizedAnswer("Gemini Fallback", retrieved);
        }
    }

    private String callOllamaOrFallback(String prompt, List<VectorStoreService.ScoredChunk> retrieved) {
        try {
            Map<String, Object> body = Map.of(
                    "model", ollamaModel,
                    "prompt", prompt,
                    "stream", false
            );
            String raw = restClient.post()
                    .uri(ollamaUrl + "/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(raw).path("response").asText();
        } catch (Exception e) {
            return buildSynthesizedAnswer("Local Offline Synthesis", retrieved);
        }
    }

    private String buildSynthesizedAnswer(String mode, List<VectorStoreService.ScoredChunk> retrieved) {
        if (retrieved.isEmpty()) {
            return "No documents are currently indexed in your knowledge base.";
        }
        VectorStoreService.ScoredChunk top = retrieved.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("Based on your indexed personal knowledge base (").append(mode).append("):\n\n");
        sb.append(top.content())
          .append(" **[Source: ").append(top.documentTitle()).append(", Chunk #").append(top.chunkIndex()).append("]**\n\n");
        if (retrieved.size() > 1) {
            VectorStoreService.ScoredChunk second = retrieved.get(1);
            sb.append("Additional related context: ")
              .append(second.content())
              .append(" ****");
        }
        return sb.toString();
    }
}
