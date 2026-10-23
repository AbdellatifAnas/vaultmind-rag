package com.portfolio.rag.dto;

import java.util.List;

public record ChatResponseDto(
        String answer,
        String providerUsed,
        boolean piiScrubbed,
        List<CitationDto> citations
) {
    public record CitationDto(
            String documentTitle,
            String docType,
            int chunkIndex,
            double similarityScore,
            String snippet
    ) {}
}
