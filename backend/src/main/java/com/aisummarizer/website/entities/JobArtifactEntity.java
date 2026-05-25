package com.aisummarizer.website.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_artifacts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class JobArtifactEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private String artifactType;

    @Column(nullable = false, length = 2000)
    private String path;

    @Column
    private Long fileSizeBytes;

    @Column
    private Long ownerId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

}
