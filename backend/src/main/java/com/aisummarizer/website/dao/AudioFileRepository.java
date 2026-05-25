package com.aisummarizer.website.dao;

import com.aisummarizer.website.entities.AudioFileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.time.Instant;
import java.util.List;
import java.util.Optional;


public interface AudioFileRepository extends JpaRepository<AudioFileEntity, Long> {
    Optional<AudioFileEntity> findById(Long id);
    Optional<AudioFileEntity> findByFileName(String fileName);
    Optional<AudioFileEntity> findByOwnerId(Long ownerId);
    Optional<AudioFileEntity> deleteByFileName(String fileName);
    Optional<List<AudioFileEntity>> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<AudioFileEntity> findByExpiresAtBefore(Instant now);
    Page<AudioFileEntity> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(a.fileSizeBytes),0) FROM AudioFileEntity a WHERE a.ownerId = :userId")
    long getTotalBytes(Long userId);

}
