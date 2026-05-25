package com.aisummarizer.website.dao;

import com.aisummarizer.website.entities.JobArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface JobArtifactRepository extends JpaRepository<JobArtifactEntity, UUID> {

    Optional<JobArtifactEntity> findFirstByJobIdAndArtifactTypeOrderByCreatedAtDesc(UUID jobId, String artifactType);
    Optional<JobArtifactEntity> findByPath(String path);
    Optional<JobArtifactEntity> findByJobIdAndArtifactType(UUID jobId, String artifactType);
    Optional<JobArtifactEntity> findByJobId(UUID jobId);

    @Query("""
        SELECT COALESCE(SUM(a.fileSizeBytes),0)
        FROM JobArtifactEntity a
        WHERE a.ownerId = :ownerId
    """)
    long sumBytesByOwnerId(Long ownerId);
}
