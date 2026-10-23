import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface DocumentSummary {
  id: number;
  title: string;
  docType: string;
  totalChunks: number;
  redactedPiiCount: number;
  ingestedAt: string;
}

interface Citation {
  documentTitle: string;
  docType: string;
  chunkIndex: number;
  similarityScore: number;
  snippet: string;
}

interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  provider?: string;
  citations?: Citation[];
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/api/v1/knowledge';

  documents: DocumentSummary[] = [];
  messages: ChatMessage[] = [
    {
      role: 'assistant',
      text: 'Welcome to VaultMind AI! Your personal documents, emails, and study notes are encrypted with AES-256 and indexed in the Vector Store. Ask me anything about your knowledge base!'
    }
  ];

  newDoc = {
    title: 'Personal_Study_Note.md',
    docType: 'NOTE',
    content: ''
  };

  question = 'How should we reduce database read latency in AWS and what is our HikariCP connection limit?';
  provider = 'GEMINI';
  scrubPii = true;
  isLoading = false;

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.http.get<DocumentSummary[]>(`${this.apiUrl}/documents`).subscribe(docs => {
      this.documents = docs;
    });
  }

  ingestDocument(): void {
    if (!this.newDoc.content.trim()) return;
    this.http.post<DocumentSummary>(`${this.apiUrl}/ingest/text`, this.newDoc).subscribe(saved => {
      this.documents.unshift(saved);
      this.newDoc.content = '';
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    const formData = new FormData();
    formData.append('file', file);
    formData.append('docType', 'PDF');

    this.http.post<DocumentSummary>(`${this.apiUrl}/ingest/file`, formData).subscribe(saved => {
      this.documents.unshift(saved);
    });
  }

  askQuestion(): void {
    if (!this.question.trim()) return;
    const q = this.question;
    this.messages.push({ role: 'user', text: q });
    this.question = '';
    this.isLoading = true;

    this.http.post<any>(`${this.apiUrl}/chat`, {
      question: q,
      topK: 3,
      scrubPii: this.scrubPii,
      provider: this.provider
    }).subscribe({
      next: (res) => {
        this.messages.push({
          role: 'assistant',
          text: res.answer,
          provider: res.providerUsed,
          citations: res.citations
        });
        this.isLoading = false;
      },
      error: () => (this.isLoading = false)
    });
  }
}
