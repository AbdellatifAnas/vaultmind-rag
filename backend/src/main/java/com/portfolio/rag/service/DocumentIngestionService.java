package com.portfolio.rag.service;

import com.portfolio.rag.dto.DocumentSummaryDto;
import com.portfolio.rag.dto.TextIngestRequest;
import com.portfolio.rag.entity.DocumentChunk;
import com.portfolio.rag.entity.KnowledgeDocument;
import com.portfolio.rag.repository.KnowledgeDocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentIngestionService {

    private static final int CHUNK_SIZE_WORDS = 60;
    private static final int CHUNK_OVERLAP_WORDS = 15;

    private final KnowledgeDocumentRepository documentRepository;
    private final PrivacyGuardService privacyGuardService;
    private final VectorStoreService vectorStoreService;

    public DocumentIngestionService(KnowledgeDocumentRepository documentRepository,
                                    PrivacyGuardService privacyGuardService,
                                    VectorStoreService vectorStoreService) {
        this.documentRepository = documentRepository;
        this.privacyGuardService = privacyGuardService;
        this.vectorStoreService = vectorStoreService;
    }

    @Transactional
    public DocumentSummaryDto ingestText(TextIngestRequest request) {
        PrivacyGuardService.RedactionResult redaction = privacyGuardService.redactSensitiveData(request.content());
        List<String> textChunks = splitIntoOverlappingChunks(redaction.sanitizedText());

        KnowledgeDocument doc = new KnowledgeDocument(
                request.title(),
                request.docType().toUpperCase(),
                textChunks.size(),
                redaction.redactionCount()
        );

        for (int i = 0; i < textChunks.size(); i++) {
            String chunkText = textChunks.get(i);
            double[] embedding = vectorStoreService.generateEmbedding(chunkText);
            String encryptedChunk = privacyGuardService.encrypt(chunkText);
            String serializedVector = vectorStoreService.serializeVector(embedding);

            doc.addChunk(new DocumentChunk(i + 1, encryptedChunk, serializedVector));
        }

        KnowledgeDocument saved = documentRepository.save(doc);
        return toSummaryDto(saved);
    }

    @Transactional
    public DocumentSummaryDto ingestFile(MultipartFile file, String docType) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "Uploaded_File";
        String extractedText;

        if (filename.toLowerCase().endsWith(".pdf")) {
            try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                extractedText = stripper.getText(pdf);
            }
            docType = "PDF";
        } else {
            extractedText = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (filename.toLowerCase().endsWith(".eml")) {
                docType = "EMAIL";
            }
        }

        return ingestText(new TextIngestRequest(filename, docType, extractedText));
    }

    public List<DocumentSummaryDto> listAllDocuments() {
        return documentRepository.findAllByOrderByIngestedAtDesc()
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }

    public List<String> splitIntoOverlappingChunks(String text) {
        String[] words = text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();
        if (words.length == 0) return chunks;

        int step = Math.max(1, CHUNK_SIZE_WORDS - CHUNK_OVERLAP_WORDS);
        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(words.length, start + CHUNK_SIZE_WORDS);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(words[i]).append(" ");
            }
            chunks.add(sb.toString().trim());
            if (end == words.length) break;
        }
        return chunks;
    }

    private DocumentSummaryDto toSummaryDto(KnowledgeDocument d) {
        return new DocumentSummaryDto(
                d.getId(), d.getTitle(), d.getDocType(),
                d.getTotalChunks(), d.getRedactedPiiCount(), d.getIngestedAt()
        );
    }
}
