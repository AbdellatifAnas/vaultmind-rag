package com.portfolio.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record TextIngestRequest(
        @NotBlank(message = "Document title is required") String title,
        @NotBlank(message = "Document type (NOTE, EMAIL, DOCUMENT) is required") String docType,
        @NotBlank(message = "Content cannot be empty") String content
) {}
