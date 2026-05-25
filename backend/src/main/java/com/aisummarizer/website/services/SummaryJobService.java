package com.aisummarizer.website.services;

import com.aisummarizer.website.dao.AudioFileRepository;
import com.aisummarizer.website.dao.JobArtifactRepository;
import com.aisummarizer.website.dao.JobRepository;
import com.aisummarizer.website.dao.TranscriptionJobRepository;
import com.aisummarizer.website.entities.*;
import com.aisummarizer.website.jobs.JobStatus;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SummaryJobService {

    private final JobRepository jobRepo;
    private final JobArtifactRepository artifactRepo;
    private final FileService fileService;
    private final AiService aiService;
    private final ChatGptAIService chatGpt;
    private final JobSseEmitterStore sse;
    private final AudioService audioService;
    private final TranscriptionJobRepository transcriptionJobRepository;
    private final FileEncrypterDecrypterService encryptionDecryptionService;

    private final List<String> mistralSupportedModels = new ArrayList<>(List.of("mistral-small","mistral-medium","mistral-large"));

    private final List<String> openAiSupportedModels = new ArrayList<>(List.of("gpt-5-mini","gpt-5"," gpt-5.2"));

    private final UserService userService;

    private final StripeService stripeService;
    private final AudioFileRepository audioFileRepository;

    public SummaryJobService(
            JobRepository jobRepo,
            JobArtifactRepository artifactRepo,
            FileService fileService,
            AiService aiService,
            ChatGptAIService chatGptAIService,
            JobSseEmitterStore sse,
            TranscriptionJobRepository  transcriptionJobRepository,
            UserService userService,
            StripeService stripeService,
            AudioFileRepository audioFileRepository, AudioService audioService,
            FileEncrypterDecrypterService encryptionDecryptionService) {
        this.jobRepo = jobRepo;
        this.artifactRepo = artifactRepo;
        this.fileService = fileService;
        this.aiService = aiService;
        this.sse = sse;
        this.transcriptionJobRepository = transcriptionJobRepository;
        this.chatGpt = chatGptAIService;
        this.userService = userService;
        this.stripeService = stripeService;
        this.audioFileRepository = audioFileRepository;
        this.audioService = audioService;
        this.encryptionDecryptionService = encryptionDecryptionService;
    }

/**
 * <p>summarizes the text</p>
 * */
    @Async("llmExecutor")
    public void summarize(UUID jobId, String transcriptFile,String transcriptionId,String model,long ownerId) {
        System.out.println(">>> summarizeToSummaryDirAsync CALLED");

//        get the job
        JobEntity job = jobRepo.findById(jobId).orElseThrow();

        try {
            job.setStatus(JobStatus.RUNNING);
            jobRepo.save(job);
            sse.send(jobId.toString(), "Summarizing…");

//
//            Path transcriptPath = Path.of("transcripts", transcriptFile);
            Path transcriptPath = fileService.getTRANSCRIPTS_DIR().resolve(transcriptFile);
            String dtext = fileService.readFile(transcriptPath);
            //decrypt it
            String text = encryptionDecryptionService.decrypt(dtext);

            Path summaryPath = Paths.get("");

            summaryPath = aiService.summarize(text,jobId,model);
            if(!summaryPath.toFile().exists()){
                System.out.println("Path: "+summaryPath+" does not exist");
            }

            JobArtifactEntity artifact = new JobArtifactEntity();
            artifact.setJobId(jobId);
            artifact.setArtifactType("SUMMARY");
            artifact.setPath(summaryPath.toString());
            long fileSize = fileService.getFileSize(transcriptPath.toAbsolutePath());
            artifact.setFileSizeBytes(fileSize);
            artifact.setOwnerId(ownerId);
            artifactRepo.save(artifact);

            TranscriptionJobEntity transcriptionJobEntity = transcriptionJobRepository.findById(UUID.fromString(transcriptionId)).orElse(null);
            if(transcriptionJobEntity == null){
                System.out.println("TranscriptionJobEntity not found");
            }
            if (transcriptionJobEntity != null) {
                String fileName = fileService.getFileName(summaryPath.toString());
                transcriptionJobEntity.setSummaryFileName(fileName);
                transcriptionJobEntity.setOwnerId(userService.getUserFromRepo().getId());


                transcriptionJobRepository.save(transcriptionJobEntity);

                job.setStatus(JobStatus.COMPLETED);
                job.setCompletedAt(Instant.now());
                job.setTranscriptionJobId(transcriptionJobEntity.getId());
                jobRepo.save(job);
            }



            System.out.println("sending a sse completed message");
            sse.send(jobId.toString(), "Completed");
            sse.complete(jobId.toString());
            System.out.println("sse completed.");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setTranscriptionJobId(UUID.fromString(transcriptionId));
            jobRepo.save(job);
            sse.send(jobId.toString(), "Failed");
            sse.complete(jobId.toString());
        }
    }

    public List<String> getSupportedModels(String model) {
        Model m = aiService.convertStringToModel(model);
        return switch (m) {
            case MISTRAL -> mistralSupportedModels; //remove this is the future
            case CHAT_GPT -> openAiSupportedModels;
//            case GEMINI -> null; //add this in the future
            default -> mistralSupportedModels; //this is default
        };
    }
}
