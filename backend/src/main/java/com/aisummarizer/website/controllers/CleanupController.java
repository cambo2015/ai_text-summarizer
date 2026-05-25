package com.aisummarizer.website.controllers;

import com.aisummarizer.website.aspects.RequiresSubscription;
import com.aisummarizer.website.dao.AudioFileRepository;
import com.aisummarizer.website.dao.JobArtifactRepository;
import com.aisummarizer.website.dao.JobRepository;
import com.aisummarizer.website.dao.TranscriptionJobRepository;
import com.aisummarizer.website.entities.AppUser;
import com.aisummarizer.website.entities.JobArtifactEntity;
import com.aisummarizer.website.entities.JobEntity;
import com.aisummarizer.website.entities.TranscriptionJobEntity;
import com.aisummarizer.website.services.FileService;
import com.aisummarizer.website.services.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RequiresSubscription
@RestController
@RequestMapping("api/cleanup")
public class CleanupController {

    private final EntityLinks entityLinks;
    private final UserService userService;
    private TranscriptionJobRepository transcriptionJobRepository;
    private JobArtifactRepository jobArtifactRepository;
    private JobRepository jobRepository;
    private FileService fileService;
    private AudioFileRepository audioFileRepository;

    public CleanupController(
            TranscriptionJobRepository transcriptionJobRepository,
            FileService fileService,
            JobArtifactRepository jobArtRepo,
            JobRepository jobRepo,
            AudioFileRepository audioFileRepo,
            @Qualifier("delegatingEntityLinks") EntityLinks entityLinks, UserService userService) {
        this.transcriptionJobRepository = transcriptionJobRepository;
        this.fileService = fileService;
        this.jobArtifactRepository = jobArtRepo;
        this.jobRepository = jobRepo;
        this.entityLinks = entityLinks;
        this.audioFileRepository = audioFileRepo;
        this.userService = userService;
    }

    @PostMapping("/delete/associated-files")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteAssociatedFiles(
            @RequestBody Map<String, String> requestParams) {

        String audioFileName = requestParams.get("fileName");
        if (audioFileName == null || audioFileName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "fileName is required"));
        }
        System.out.println("deleteAssociatedFiles: 1. audio file name:" + audioFileName);

        AppUser user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Must be logged in."));
        }

        long userId = user.getId();

        if (!fileService.isOwnerOfAudioFile(audioFileName, userId, audioFileRepository)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Not authorized"));
        }

        Path audioFilePath = fileService.getAUDIO_DIR()
                .resolve(audioFileName)
                .toAbsolutePath()
                .normalize();

        TranscriptionJobEntity job = transcriptionJobRepository
                .findByAudioPath(audioFilePath.toString())
                .orElse(null);

        Path transcriptionPath = null;
        Path summaryPath = null;
        UUID jobId = null;

        if (job != null) {
            jobId = job.getId();

            if (job.getTranscriptFileName() != null &&
                    !job.getTranscriptFileName().isBlank()) {
                transcriptionPath = Paths.get(job.getTranscriptFileName())
                        .toAbsolutePath()
                        .normalize();
            }

            if (job.getSummaryFileName() != null &&
                    !job.getSummaryFileName().isBlank()) {
                summaryPath = Paths.get(job.getSummaryFileName())
                        .toAbsolutePath()
                        .normalize();
            }
        }

        System.out.println("deleteAssociatedFiles: 2. audioFilePath"+audioFilePath);
        boolean audioDeleted = fileService.deleteFile(audioFilePath);
        if (!audioDeleted) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to delete audio file"));
        }

        if (transcriptionPath != null) fileService.deleteFile(transcriptionPath);
        if (summaryPath != null) fileService.deleteFile(summaryPath);

        if (jobId != null) {
            jobArtifactRepository.findByJobId(jobId)
                    .ifPresent(jobArtifactRepository::delete);
            jobRepository.findById(jobId)
                    .ifPresent(jobRepository::delete);
        }

        if (job != null) transcriptionJobRepository.delete(job);
        audioFileRepository.deleteByFileName(audioFileName);

        return ResponseEntity.ok(Map.of("success", "Files deleted successfully"));
    }

}
