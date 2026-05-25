package com.aisummarizer.website.entities;

import com.aisummarizer.website.jobs.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String type;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    @Column(length = 2000)
    private String errorMessage;

    @Column
    private UUID transcriptionJobId;

    private Instant createdAt = Instant.now();
    private Instant completedAt;


}
