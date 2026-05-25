package com.aisummarizer.website.controllers;

import com.aisummarizer.website.aspects.RequiresStorageQuota;
import com.aisummarizer.website.aspects.RequiresSubscription;
import com.aisummarizer.website.dao.JobArtifactRepository;
import com.aisummarizer.website.dao.JobRepository;
import com.aisummarizer.website.dao.TranscriptionJobRepository;
import com.aisummarizer.website.dto.CommonWordRequest;
import com.aisummarizer.website.entities.*;
import com.aisummarizer.website.jobs.JobStatus;
import com.aisummarizer.website.services.*;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for Whisper text extraction jobs.
 *
 * <p>
 * Provides endpoints to:
 * <ul>
 *   <li>Create a Whisper job</li>
 *   <li>Stream job progress</li>
 *   <li>Retrieve extracted transcript</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/text-extractor")
public class TextExtractorController {

    private final JobRepository jobRepository;
    private final JobArtifactRepository artifactRepository;
    private final TextExtractorService textExtractorService;
    private final JobSseEmitterStore sse;
    private final TranscriptionJobRepository transcriptionJobRepository;
    private final FileService fileService;
    private final UserService userService;
    private  final FileEncrypterDecrypterService fileEncrypterDecrypterService;

    @Value("${app.use-online}")
    private boolean useOnline;

    public TextExtractorController(
            JobRepository jobRepository,
            JobArtifactRepository artifactRepository,
            TextExtractorService textExtractorService,
            JobSseEmitterStore sse,
            TranscriptionJobRepository transcriptionJobRepository, FileService fileService, UserService userService,FileEncrypterDecrypterService fileEncrypterDecrypterService) {
        this.jobRepository = jobRepository;
        this.artifactRepository = artifactRepository;
        this.textExtractorService = textExtractorService;
        this.sse = sse;
        this.transcriptionJobRepository = transcriptionJobRepository;
        this.fileService = fileService;
        this.userService = userService;
        this.fileEncrypterDecrypterService = fileEncrypterDecrypterService;

    }

    /**
     * 1️⃣ Creates a Whisper text extraction job.
     *
     * @param body JSON containing:
     *             <ul>
     *               <li>filepath – path to audio file</li>
     *             </ul>
     * @return jobId
     */
    @PostMapping("/create")
    @RequiresSubscription
    @RequiresStorageQuota
    public ResponseEntity<Map<String, String>> create(
            @RequestBody Map<String, String> body
    ) {
        String audioName = body.get("fileName");
        if (audioName == null || audioName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "fileName is required"));
        }

        Path p = fileService.getAUDIO_DIR()
                .toAbsolutePath()
                .resolve(audioName)
                .normalize();

        System.out.println("1. Starting to create audio file");
        System.out.println("Starting to create transcription file");
        System.out.println("Here is the audio directory path"+p.toString());
//      create a job entity
        JobEntity job = new JobEntity();
        System.out.println(job);
        job.setType("WHISPER");
        jobRepository.save(job);


        //create a job transcription entity
        TranscriptionJobEntity tj = new TranscriptionJobEntity();
        tj.setId(job.getId());
        tj.setAudioPath(p.toString());
        tj.setStatus(JobStatus.PENDING);

        transcriptionJobRepository.save(tj);

//        get the text from audio
        WhisperType whisperType;
        if (useOnline) {
            whisperType = WhisperType.INTERNET;
        }else{
            whisperType = WhisperType.LOCAL_SERVER;
        }
        AppUser user = userService.getCurrentUser();
        String userStripeUserId = user.getStripeCustomerId();
        boolean isSubscribed = user.getSubscribed();
        textExtractorService.extractText(job.getId(), whisperType,isSubscribed,userStripeUserId,user.getId());//if you want to use offline change whispertype  to INTERNET

        //return result
        return ResponseEntity.accepted()
                .body(Map.of("jobId", job.getId().toString()));
    }

    /**
     * 2️⃣ Streams Whisper job progress updates via SSE.
     */
    @GetMapping("/jobs/{jobId}/stream")
    public SseEmitter stream(@PathVariable String jobId) {
        return sse.create(jobId);
    }

    /**
     * 3️⃣ Retrieves extracted transcript text.
     */
    @PostMapping("/get")
    public ResponseEntity<String> getTranscript(
            @RequestBody Map<String, String> body
    ) throws Exception {

        UUID jobId = UUID.fromString(body.get("jobId"));

        JobArtifactEntity artifact =
                artifactRepository
                        .findFirstByJobIdAndArtifactTypeOrderByCreatedAtDesc(jobId, "TRANSCRIPT")
                        .orElse(null);
        if (artifact == null) {
            //return not found with body "error not found"
            return ResponseEntity.badRequest().body("Item does not exist in the database.");
        }

        Path artifactPath = Path.of(artifact.getPath());
        JobArtifactEntity jobArtafactEntity =  artifactRepository.findByPath(artifactPath.toString()).orElse(null);
        boolean isOwner = fileService.isOwnerOfFileTranscriptionJob(jobArtafactEntity, transcriptionJobRepository);
        if(isOwner) {
            String encryptedText = Files.readString(artifactPath);
            String decrypted = fileEncrypterDecrypterService.decrypt(encryptedText);
            return ResponseEntity.ok(decrypted);
        }
        return ResponseEntity.badRequest().body("Can't retrieve the file. You are not the owner.");
    }

    @PostMapping("/common-words")
    public CompletableFuture<ResponseEntity<List<String>>> getCommonWords(
            @RequestBody CommonWordRequest request
    ) {
        String transcriptionFileName = request.getTranscriptionFileName();
        Integer top = request.getTop();
        if (transcriptionFileName == null || transcriptionFileName.isBlank()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(List.of("transcriptionFileName is required"))
            );
        }
        if(top == null){
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(List.of("top is required")));
        }

        return textExtractorService
                .getMostFreqWordsInTranscription(transcriptionFileName, top)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(List.of(ex.getMessage()))
                );
    }
}
