package com.aisummarizer.website.dao;

import com.aisummarizer.website.entities.TranscriptionJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TranscriptionJobRepository extends JpaRepository<TranscriptionJobEntity, UUID> {
    public Optional<TranscriptionJobEntity> findByAudioPath(String audioPath);

    public Optional<TranscriptionJobEntity> findByTranscriptFileName(String string);

    Optional<TranscriptionJobEntity> findBySummaryFileName(String string);
}
