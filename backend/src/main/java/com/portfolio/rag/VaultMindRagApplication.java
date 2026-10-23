package com.portfolio.rag;

import com.portfolio.rag.dto.TextIngestRequest;
import com.portfolio.rag.service.DocumentIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class VaultMindRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(VaultMindRagApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedSampleKnowledgeBase(DocumentIngestionService ingestionService) {
        return args -> {
            ingestionService.ingestText(new TextIngestRequest(
                    "AWS_Cloud_Architecture_Notes.md",
                    "NOTE",
                    "AWS Backend Architecture Study Notes: Use Amazon ECS with Fargate for serverless container orchestration. "
                    + "For relational databases, Amazon RDS Multi-AZ provides high availability and automatic failover. "
                    + "To reduce database read latency, place Amazon ElastiCache (Redis) in front of RDS. "
                    + "Sensitive credentials must be stored in AWS Secrets Manager, never hardcoded in application.yml."
            ));

            ingestionService.ingestText(new TextIngestRequest(
                    "Project_Onboarding_Email.eml",
                    "EMAIL",
                    "From: tech-lead@company.com\nSubject: Spring Boot Production Security Checklist\n\n"
                    + "Hi Team, remember that our microservice deployment deadline is Friday. "
                    + "All REST APIs must enforce stateless JWT authentication and rate limiting. "
                    + "If you need emergency server access, call the on-call engineer at +48 500-600-700 or email admin@internal.org. "
                    + "Also ensure database connection pooling via HikariCP is capped at 20 maximum connections."
            ));

            ingestionService.ingestText(new TextIngestRequest(
                    "Distributed_Systems_Exam_Prep.txt",
                    "DOCUMENT",
                    "CAP Theorem & Microservices: A distributed data store can only simultaneously provide two out of three guarantees: "
                    + "Consistency, Availability, and Partition Tolerance (CAP). "
                    + "In event-driven architectures using Apache Kafka, idempotent consumers prevent duplicate message processing. "
                    + "Vector databases index high-dimensional embeddings using HNSW (Hierarchical Navigable Small World) graphs for fast approximate nearest neighbor (ANN) search."
            ));
        };
    }
}
