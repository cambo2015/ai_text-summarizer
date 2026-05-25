
package com.aisummarizer.website.entities;

import com.aisummarizer.website.jobs.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transcription_jobs")
@Getter
@Setter
public class TranscriptionJobEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private String audioPath;

    @Column
    private String transcriptFileName;

    @Column
    private String summaryFileName;

    @Column(length = 32)
    private String language;

    @Column(length = 64)
    private String modelUsed;

    @Column(nullable = true)
    private Long ownerId;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    @Column
    private Boolean usageReported = false;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
        status = JobStatus.PENDING;
    }
}
