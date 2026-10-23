package com.portfolio.rag.controller;

import com.portfolio.rag.dto.ChatQueryRequest;
import com.portfolio.rag.dto.ChatResponseDto;
import com.portfolio.rag.dto.DocumentSummaryDto;
import com.portfolio.rag.dto.TextIngestRequest;
import com.portfolio.rag.service.DocumentIngestionService;
import com.portfolio.rag.service.RagChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final DocumentIngestionService ingestionService;
    private final RagChatService ragChatService;

    public KnowledgeController(DocumentIngestionService ingestionService, RagChatService ragChatService) {
        this.ingestionService = ingestionService;
        this.ragChatService = ragChatService;
    }

    @PostMapping("/ingest/text")
    public ResponseEntity<DocumentSummaryDto> ingestText(@Valid @RequestBody TextIngestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingestionService.ingestText(request));
    }

    @PostMapping(value = "/ingest/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentSummaryDto> ingestFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "docType", defaultValue = "DOCUMENT") String docType) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingestionService.ingestFile(file, docType));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentSummaryDto>> listDocuments() {
        return ResponseEntity.ok(ingestionService.listAllDocuments());
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        ingestionService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDto> chatWithKnowledgeBase(@Valid @RequestBody ChatQueryRequest request) {
        return ResponseEntity.ok(ragChatService.askQuestion(request));
    }
}
