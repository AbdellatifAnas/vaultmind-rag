package com.portfolio.rag.dto;

import java.time.LocalDateTime;

public record DocumentSummaryDto(
        Long id,
        String title,
        String docType,
        int totalChunks,
        int redactedPiiCount,
        LocalDateTime ingestedAt
) {}
