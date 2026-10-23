package com.portfolio.rag.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int chunkIndex;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String encryptedContent;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String serializedEmbedding;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_id")
    @JsonIgnore
    private KnowledgeDocument document;

    public DocumentChunk() {}

    public DocumentChunk(int chunkIndex, String encryptedContent, String serializedEmbedding) {
        this.chunkIndex = chunkIndex;
        this.encryptedContent = encryptedContent;
        this.serializedEmbedding = serializedEmbedding;
    }

    public Long getId() { return id; }
    public int getChunkIndex() { return chunkIndex; }
    public String getEncryptedContent() { return encryptedContent; }
    public String getSerializedEmbedding() { return serializedEmbedding; }
    public KnowledgeDocument getDocument() { return document; }
    public void setDocument(KnowledgeDocument document) { this.document = document; }
}
