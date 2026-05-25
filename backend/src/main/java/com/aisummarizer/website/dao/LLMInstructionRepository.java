package com.aisummarizer.website.dao;

import com.aisummarizer.website.entities.LLMInstructionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LLMInstructionRepository extends JpaRepository<LLMInstructionEntity, UUID> {
    Optional<LLMInstructionEntity> findById(UUID id);
    Optional<LLMInstructionEntity> findByFileName(String fileName);
    Optional<LLMInstructionEntity> findByOwnerId(Long ownerId);
}
