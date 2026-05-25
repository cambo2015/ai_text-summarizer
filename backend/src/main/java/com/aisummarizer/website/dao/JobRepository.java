package com.aisummarizer.website.dao;

import com.aisummarizer.website.entities.JobEntity;
import com.aisummarizer.website.jobs.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {
    public Optional<JobEntity> findByTranscriptionJobId(UUID id);
    public Optional<List<JobEntity>> findJobEntitiesByStatus(JobStatus status);
}

