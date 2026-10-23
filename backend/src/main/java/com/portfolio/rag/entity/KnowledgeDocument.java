package com.portfolio.rag.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String docType; // PDF, EMAIL, NOTE, DOCUMENT

    private int totalChunks;
    private int redactedPiiCount;
    private LocalDateTime ingestedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DocumentChunk> chunks = new ArrayList<>();

    public KnowledgeDocument() {}

    public KnowledgeDocument(String title, String docType, int totalChunks, int redactedPiiCount) {
        this.title = title;
        this.docType = docType;
        this.totalChunks = totalChunks;
        this.redactedPiiCount = redactedPiiCount;
        this.ingestedAt = LocalDateTime.now();
    }

    public void addChunk(DocumentChunk chunk) {
        chunks.add(chunk);
        chunk.setDocument(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    public int getRedactedPiiCount() { return redactedPiiCount; }
    public void setRedactedPiiCount(int redactedPiiCount) { this.redactedPiiCount = redactedPiiCount; }
    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }
    public List<DocumentChunk> getChunks() { return chunks; }
}
