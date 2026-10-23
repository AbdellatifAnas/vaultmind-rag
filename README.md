# 🧠 VaultMind AI — Privacy-First Personal Knowledge Base & RAG Engine

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-brightgreen)
![Angular](https://img.shields.io/badge/Angular-17-red)
![Vector DB](https://img.shields.io/badge/Vector_Search-Cosine_Similarity_%2F_pgvector-blue)
![Security](https://img.shields.io/badge/Privacy-AES--256_%2B_PII_Redaction-critical)

An enterprise-grade **Retrieval-Augmented Generation (RAG)** platform built with **Java 17**, **Spring Boot 3**, and **Angular 17**. VaultMind AI ingests personal PDFs, Markdown study notes, and `.eml` emails, chunks and embeds them into a Vector Store, and provides conversational Q&A with **strict inline source citations** and **automated PII privacy scrubbing**.

## 🚀 Key Backend & AI Engineering Highlights
- **Custom Vector Engine & Semantic Search:** Implements dense vector embeddings (`text-embedding-004` + deterministic local fallback embeddings) and **Cosine Similarity Top-K retrieval** to surface the exact paragraphs relevant to a user's query.
- **Multi-Format Document Ingestion Pipeline:** Extracts and normalizes text from **PDF documents (Apache PDFBox)**, **Emails (`.eml`)**, and **Markdown/Text notes** using an overlapping sliding-window chunker.
- **Privacy & Security Architecture:**
  - **PII Redaction Engine:** Automatically detects and masks credit cards, phone numbers, email addresses, and national IDs *before* sending context chunks to external LLMs.
  - **AES-256 Encryption at Rest:** Encrypts sensitive personal document chunks in the database.
  - **Hybrid Cloud / Offline LLM Toggle:** Seamlessly switch between **Google Gemini 2.5 Flash** and local **Ollama (`llama3`)** for 100% air-gapped privacy.
- **Grounded Responses with Inline Citations:** Enforces strict grounding on retrieved context chunks and returns exact citations (`[DocTitle - Chunk #]`) alongside cosine similarity confidence scores.

## ⚡ Quick Start
1. Open the root folder in **IntelliJ IDEA** and load `backend/pom.xml` as a Maven project.
2. Run `VaultMindRagApplication` — starts immediately on **`http://localhost:8081`** with pre-loaded sample documents.
3. Open Swagger UI at **`http://localhost:8081/swagger-ui.html`** or start the Angular UI (`cd frontend && npm install && npm start` -> `http://localhost:4200`).
