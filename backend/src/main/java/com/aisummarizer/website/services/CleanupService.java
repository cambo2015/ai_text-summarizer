package com.aisummarizer.website.services;


import com.aisummarizer.website.dao.AudioFileRepository;
import com.aisummarizer.website.dao.JobArtifactRepository;
import com.aisummarizer.website.dao.JobRepository;
import com.aisummarizer.website.dao.TranscriptionJobRepository;
import com.aisummarizer.website.entities.AudioFileEntity;
import com.aisummarizer.website.entities.JobArtifactEntity;
import com.aisummarizer.website.entities.JobEntity;
import com.aisummarizer.website.entities.TranscriptionJobEntity;
import com.aisummarizer.website.jobs.JobStatus;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CleanupService {
    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

    private final AudioFileRepository audioFileRepository;
    private final FileService  fileService;
    private final TranscriptionJobRepository transcriptionJobRepo;
    private final JobRepository jobRepository;
    private final JobArtifactRepository jobArtifactRepository;


    public CleanupService(
            AudioFileRepository audioFileRepository,
            FileService fileService,
            TranscriptionJobRepository transcriptionJobRepo,
            JobRepository jobRepository, JobArtifactRepository jobArtifactRepository) {
        this.audioFileRepository = audioFileRepository;
        this.fileService = fileService;
        this.transcriptionJobRepo = transcriptionJobRepo;
        this.jobRepository = jobRepository;
        this.jobArtifactRepository = jobArtifactRepository;
    }

    @Scheduled(cron="0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredFiles(){
       removeAllExpired();
       removeAllFailedJobs();
    }

    private void removeAllExpired(){
        Instant now = Instant.now();

        List<AudioFileEntity> expired = audioFileRepository.findByExpiresAtBefore(now);

        //delete audio file and audio file metadata
        for (AudioFileEntity audioFileEntity : expired) {
            try{
                String audioFileName = audioFileEntity.getFileName();
                Path p = fileService.getAUDIO_DIR()
                        .resolve(audioFileName)
                        .toAbsolutePath();
                TranscriptionJobEntity tjobEntity =  transcriptionJobRepo.findByAudioPath(p.toString()).orElse(null);
                if(tjobEntity == null){
                    log.debug("No transcription job for audio {}",audioFileEntity.getId());
                }
                //delete files
                deleteTranscriptionFileIfExists(tjobEntity);
                deleteSummaryFileIfExists(tjobEntity);
                deleteAudioFile(audioFileEntity);

                //delete jobs
                deleteJobArtifact(tjobEntity);
                deleteJob(tjobEntity);
                deleteTranscriptionJob(tjobEntity);

                //delete audio entity
                audioFileRepository.delete(audioFileEntity);
                log.info("Cleanup completed. {} audio files removed", expired.size());
            }
            catch(Exception e){
                log.error("Cleanup failed for audioId={}",audioFileEntity.getId(),e);
            }

        }
    }

    /// <p>Deletes a summary file if it exists</p>
    private void deleteSummaryFileIfExists(TranscriptionJobEntity transcriptionJobEntity){
        if(transcriptionJobEntity == null) return;
        //get transcription job
        String summaryFileName = transcriptionJobEntity.getSummaryFileName();
        if( summaryFileName != null && !summaryFileName.isBlank()){//if summaryFile is not blank
            Path p = fileService.getSUMMARY_DIR().resolve(summaryFileName).toAbsolutePath(); // get the directory
            fileService.deleteFile(p);
        }
    }

    private void deleteTranscriptionFileIfExists(TranscriptionJobEntity transcriptionJobEntity){
        if(transcriptionJobEntity == null) return;
        String transcriptionFileName = transcriptionJobEntity.getTranscriptFileName();
        if( transcriptionFileName != null && !transcriptionFileName.isBlank()){
            Path p = fileService.getTRANSCRIPTS_DIR().resolve(transcriptionFileName).toAbsolutePath();
            fileService.deleteFile(p);
        }
    }

    private void deleteJob(TranscriptionJobEntity tjEntity){
        if(tjEntity == null) return;
        //transcription job has the same id as the job table
        UUID id = tjEntity.getId();
        JobEntity jobEntity =  jobRepository.findById(id).orElse(null);
        if(jobEntity != null) {
            jobRepository.delete(jobEntity);
        }
        //delete summary job row if exists
        JobEntity jobEntity2 =  jobRepository.findByTranscriptionJobId(tjEntity.getId()).orElse(null);
        if(jobEntity2 == null) return;
        jobRepository.delete(jobEntity2);
    }

    private void deleteJobArtifact(TranscriptionJobEntity tjentity){
        if(tjentity == null) return;
        // delete TRANSCRIPTION
        String transcriptName = tjentity.getTranscriptFileName();
        if(transcriptName != null && !transcriptName.isBlank()){
            Path transPath = fileService.getTRANSCRIPTS_DIR().resolve(transcriptName).toAbsolutePath();
            JobArtifactEntity artEntity = jobArtifactRepository.findByPath(transPath.toString()).orElse(null);
            if(artEntity != null) {
                jobArtifactRepository.delete(artEntity);
            }
        }
        // delete SUMMARY
        String summaryFileName = tjentity.getSummaryFileName();
        if(summaryFileName != null && !summaryFileName.isBlank()){
            Path summaryPath = fileService.getSUMMARY_DIR().resolve(summaryFileName).toAbsolutePath();
            JobArtifactEntity artifactEntity = jobArtifactRepository.findByPath(summaryPath.toString()).orElse(null);
            if(artifactEntity != null){
                jobArtifactRepository.delete(artifactEntity);
            }
        }
    }

    private void deleteTranscriptionJob(TranscriptionJobEntity tjentity){
        if(tjentity == null) return;
        transcriptionJobRepo.delete(tjentity);
    }

    private void deleteAudioFile(AudioFileEntity audioEntity) {
        if (audioEntity == null) return;
        String audioFileName = audioEntity.getFileName();
        if(audioFileName == null || audioFileName.isBlank()) return;
        Path p = fileService.getAUDIO_DIR().resolve(audioFileName);
        fileService.deleteFile(p);
    }

    private void removeAllFailedJobs(){
        List<JobEntity> entities =  jobRepository.findJobEntitiesByStatus(JobStatus.FAILED).orElse(null);
        if(entities == null || entities.isEmpty() ) return;
        jobRepository.deleteAll(entities);
    }
}
