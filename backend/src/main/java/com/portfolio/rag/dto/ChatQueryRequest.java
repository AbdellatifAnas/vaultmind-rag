package com.portfolio.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatQueryRequest(
        @NotBlank(message = "Question cannot be empty") String question,
        int topK,
        boolean scrubPii,
        String provider // "GEMINI" or "OLLAMA"
) {}
