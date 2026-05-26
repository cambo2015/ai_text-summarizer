package com.aisummarizer.website.services;

import com.aisummarizer.website.dao.JobArtifactRepository;
import com.aisummarizer.website.dao.JobRepository;
import com.aisummarizer.website.dao.TranscriptionJobRepository;
import com.aisummarizer.website.entities.JobArtifactEntity;
import com.aisummarizer.website.entities.JobEntity;
import com.aisummarizer.website.entities.TranscriptionJobEntity;
import com.aisummarizer.website.entities.WhisperType;
import com.aisummarizer.website.jobs.JobStatus;
import com.stripe.exception.StripeException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Executes Whisper-based text extraction jobs.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Run Whisper asynchronously</li>
 *   <li>Update job lifecycle status</li>
 *   <li>Persist transcript location as a JobArtifact</li>
 *   <li>Emit SSE progress updates</li>
 * </ul>
 * </p>
 */
@Service
public class TextExtractorService {

    private final TranscriptionJobRepository transcriptionJobRepository;
    private final JobRepository mainJobRepository;
    private final JobArtifactRepository artifactRepository;
    private final WhisperTextExtractor whisper;
    private final JobSseEmitterStore sse;
    private final UserService userService;
    private final FileService fileService;
    private final StripeService stripeService;
    private final FileEncrypterDecrypterService fileEncrypterDecrypterService;

    public TextExtractorService(
            TranscriptionJobRepository jobRepository,
            JobRepository mainJobRepository,
            JobArtifactRepository artifactRepository,
            WhisperTextExtractor whisper,
            JobSseEmitterStore sse,
            UserService userService,
            FileService fileService,
            StripeService stripeService, FileEncrypterDecrypterService fileEncrypterDecrypterService) {
        this.transcriptionJobRepository = jobRepository;
        this.mainJobRepository = mainJobRepository;
        this.artifactRepository = artifactRepository;
        this.whisper = whisper;
        this.sse = sse;
        this.userService = userService;
        this.fileService = fileService;
        this.stripeService = stripeService;
        this.fileEncrypterDecrypterService = fileEncrypterDecrypterService;
    }

    @Async("whisperExecutor")
    public void extractText(UUID jobId,WhisperType whisperType,boolean isSubscribed, String stripeCustomerId,Long ownerId) {


        TranscriptionJobEntity transcriptionJobEntity =
                transcriptionJobRepository.findById(jobId)
                        .orElseThrow(() ->
                                new IllegalStateException("Transcription Job not found: " + jobId));

        JobEntity mainJob =
                mainJobRepository.findById(jobId)
                        .orElseThrow(() ->
                                new IllegalStateException("Main job not found: " + jobId));

        try {
            // RUNNING
            transcriptionJobEntity.setStatus(JobStatus.RUNNING);
            mainJob.setStatus(JobStatus.RUNNING);
            transcriptionJobRepository.save(transcriptionJobEntity);
            mainJobRepository.save(mainJob);

            sse.send(jobId.toString(), "Running Whisper…");

            Path audioPath = Path.of(transcriptionJobEntity.getAudioPath());
            System.out.println("Audio path: " + audioPath.toAbsolutePath());
            System.out.println("Audio exists: " + Files.exists(audioPath));
            System.out.println("Audio size: " + Files.size(audioPath));
            System.out.println("Whisper type: " + whisperType);
            // Whisper
            Path transcriptPath = whisper.extract(
                    Path.of(transcriptionJobEntity.getAudioPath()),
                    jobId,
                    whisperType
            );

            mainJob.setTranscriptionJobId(transcriptionJobEntity.getId());

            // Artifact
            JobArtifactEntity artifact = new JobArtifactEntity();
            artifact.setJobId(jobId);
            artifact.setArtifactType("TRANSCRIPT");
            artifact.setPath(transcriptPath.toString());
            long fileSize = fileService.getFileSize(transcriptPath.toAbsolutePath());
            artifact.setFileSizeBytes(fileSize);
            artifact.setOwnerId(ownerId);
            artifactRepository.save(artifact);

            String transcriptionName = fileService.getFileName(transcriptPath.toString());
            // COMPLETED
            transcriptionJobEntity.setTranscriptFileName(transcriptionName);
//            transcriptionJobEntity.setOwnerId(userService.getUserFromRepo().getId());
            transcriptionJobEntity.setOwnerId(ownerId);
            transcriptionJobEntity.setStatus(JobStatus.COMPLETED);
            transcriptionJobEntity.setCompletedAt(Instant.now());

            mainJob.setStatus(JobStatus.COMPLETED);
            mainJob.setCompletedAt(Instant.now());

            double hours = 0.0;
            //added code
            if(isSubscribed && stripeCustomerId != null && !transcriptionJobEntity.getUsageReported()){
                System.out.println("starting to report hours:");
                try {
                    String audioStringPath = transcriptionJobEntity.getAudioPath();//this is an absolute path to the object
                    Path p = Path.of(audioStringPath);
                    hours = fileService.getAudioDurationInHours(p); //get audio duration length

                    if(hours != 0.0){
                        stripeService.reportUsage(stripeCustomerId, hours); //get the length of the audio file.
                        transcriptionJobEntity.setUsageReported(true);
                        System.out.println("Hours reported");
                    }
                } catch (IOException | InterruptedException | StripeException e) {
                    System.out.println("SummaryJobService summarize() 1. Could not report the stripe usage.See below message:");
                    e.printStackTrace();
                }
            }
            transcriptionJobRepository.save(transcriptionJobEntity);
            mainJobRepository.save(mainJob);

            sse.send(jobId.toString(), "Completed");
            sse.complete(jobId.toString());

        } catch (Exception e) {
            transcriptionJobEntity.setStatus(JobStatus.FAILED);
//            transcriptionJobEntity.setOwnerId(userService.getUserFromRepo().getId());
            transcriptionJobEntity.setOwnerId(ownerId);
            transcriptionJobEntity.setErrorMessage(e.getMessage());

            mainJob.setStatus(JobStatus.FAILED);
            mainJob.setErrorMessage(e.getMessage());

            transcriptionJobRepository.save(transcriptionJobEntity);
            mainJobRepository.save(mainJob);

            sse.send(jobId.toString(), "Failed: " + e.getMessage());
            sse.complete(jobId.toString());
            e.printStackTrace();
        }
    }

    @Async("commonWordExecutor")
    public CompletableFuture<List<String>> getMostFreqWordsInTranscription(
           String transcriptionFileName,
           int top
    ) {
        try{
            Path pythonScript = fileService.getPYTHON_DIR().resolve("freqWords.py");
            Path transcriptionFile = fileService.getTRANSCRIPTS_DIR().resolve(transcriptionFileName);

            String encryptedText = Files.readString(transcriptionFile, StandardCharsets.UTF_8);
            String decryptedText = fileEncrypterDecrypterService.decrypt(encryptedText);
            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    pythonScript.toAbsolutePath().toString(),
                    "--top",
                    String.valueOf(top)
            );


            pb.redirectErrorStream(true);

            Process process = pb.start();

            try(BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(),StandardCharsets.UTF_8))){
                writer.write(decryptedText);
                writer.flush();
            }

            List<String> outputLines = new ArrayList<>();

            try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine())!= null){
                    outputLines.add(line);
                }
            }

            int exitCode = process.waitFor();

            if(exitCode != 0){
                return CompletableFuture.failedFuture(new RuntimeException("(1) Python process exited with code "+exitCode));
            }
            return CompletableFuture.completedFuture(outputLines);
        }
        catch (Exception e){
            System.out.println("(2) Failed to get the most common words"+e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}

