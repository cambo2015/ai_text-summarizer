package com.aisummarizer.website.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="audio_files")
@Getter
@Setter
public class AudioFileEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String fileName;

    @Column
    private String originalFileName;

    @Column
    private Long ownerId;

    @CreationTimestamp
    private Instant createdAt;

    @Column
    private Instant expiresAt;//30 days from upload.

    @Column
    private double duration;

    @Column
    long fileSizeBytes;
}
