package com.aisummarizer.website.services;

import com.aisummarizer.website.dao.AudioFileRepository;
import com.aisummarizer.website.dao.UserRepository;
import com.aisummarizer.website.dto.Tiers;
import com.aisummarizer.website.entities.AppUser;
import com.aisummarizer.website.entities.AudioFileEntity;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AudioService {

    public final AudioFileRepository audioFileRepository;

    public AudioService(AudioFileRepository audioFileRepository) {

        this.audioFileRepository = audioFileRepository;
    }

    public boolean isAudioFile(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        return name.endsWith(".mp3")
                || name.endsWith(".wav")
                || name.endsWith(".ogg")
                || name.endsWith(".m4a");
    }

    private static final Logger log = LoggerFactory.getLogger(AudioService.class);


    /// <p>Do not use. Mp3 file size to big</p>
    public CompletableFuture<Path> convertToMp3(Path inputFile) throws IOException, InterruptedException {

        String outputFileName = stripExtension(inputFile.getFileName().toString()) + ".mp3";
        Path outputFile = inputFile.getParent().resolve(outputFileName);

        List<String> command = List.of(
                "ffmpeg",
                "-y",                 // overwrite if exists
                "-i", inputFile.toAbsolutePath().toString(),
                "-vn",                // no video
                "-acodec", "libmp3lame",
                "-ab", "128k",
                "-ar", "44100",
                outputFile.toAbsolutePath().toString()
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // combine stdout + stderr

        Process process = pb.start();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(log::debug);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {

            throw new IllegalStateException("ffmpeg failed with exit code " + exitCode);
//            return CompletableFuture.failedFuture(new IllegalStateException("ffmpeg failed with exit code " + exitCode));
        }
        return CompletableFuture.completedFuture(outputFile);
    }

    public CompletableFuture<Path> convertToWebM(Path inputFile) throws IOException, InterruptedException {

        String outputFileName = stripExtension(inputFile.getFileName().toString()) + ".webm";
        Path outputFile = inputFile.getParent().resolve(outputFileName);

        List<String> command = List.of(
                "ffmpeg",
                "-y",                                      // overwrite if exists
                "-threads","1",
                "-i", inputFile.toAbsolutePath().toString(),
                "-vn",                                     // no video
                "-c:a", "libopus",                         // Opus codec
                "-b:a", "32k",                             // bitrate (great balance)
                "-vbr", "on",                              // variable bitrate (recommended)
                "-application","voip",
                "-compression_level", "10",
                "-f","webm", //force webm container
                outputFile.toAbsolutePath().toString()     // should end with .opus
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // combine stdout + stderr

        Process process = pb.start();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(log::debug);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {

            throw new IllegalStateException("ffmpeg failed with exit code " + exitCode);
//            return CompletableFuture.failedFuture(new IllegalStateException("ffmpeg failed with exit code " + exitCode));
        }
        return CompletableFuture.completedFuture(outputFile);
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot == -1) ? filename : filename.substring(0, dot);
    }


    @Transactional
    public void saveMetadata(String fileName, Long userId) {

        AudioFileEntity entity = new AudioFileEntity();
        entity.setFileName(fileName);
        entity.setOwnerId(userId);
        //set time
        entity.setExpiresAt(thirtyDaysFromNow());
        audioFileRepository.save(entity);
    }

    @Transactional
    public void saveMetadata(MultipartFile file,String fileName, Long userId,long fileSize) {
        AudioFileEntity entity = new AudioFileEntity();
        entity.setFileName(fileName);
        entity.setOwnerId(userId);
        entity.setOriginalFileName(file.getOriginalFilename());
        long currentBytes = entity.getFileSizeBytes();
        entity.setFileSizeBytes(currentBytes + fileSize);


        entity.setExpiresAt(thirtyDaysFromNow());
        audioFileRepository.save(entity);
    }

    private Instant thirtyDaysFromNow(){
        Instant now = Instant.now();
        return now.plus(30, ChronoUnit.DAYS);
    }



}
