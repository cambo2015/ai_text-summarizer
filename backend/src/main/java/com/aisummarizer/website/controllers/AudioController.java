package com.aisummarizer.website.controllers;


import com.aisummarizer.website.aspects.RequiresStorageQuota;
import com.aisummarizer.website.aspects.RequiresSubscription;
import com.aisummarizer.website.dao.AudioFileRepository;
import com.aisummarizer.website.dao.JobArtifactRepository;
import com.aisummarizer.website.dao.TranscriptionJobRepository;
import com.aisummarizer.website.dto.AudioFileDto;
import com.aisummarizer.website.entities.AppUser;
import com.aisummarizer.website.entities.AudioFileEntity;
import com.aisummarizer.website.entities.TranscriptionJobEntity;
import com.aisummarizer.website.services.AudioService;
import com.aisummarizer.website.services.FileService;
import com.aisummarizer.website.services.StripeService;
import com.aisummarizer.website.services.UserService;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.springframework.core.io.Resource;

import static com.aisummarizer.website.helpers.AllowedExtensions.ALLOWED_EXTENSIONS;

@RestController
@RequestMapping("/api/audio")
public class AudioController {


    private final FileService fileService;
    private final AudioService audioService;
    private final JobArtifactRepository jobArtifactRepository;
    private final TranscriptionJobRepository transcriptionJobRepository;
    private final AudioFileRepository  audioFileRepository;
    private final UserService userService;
    private final StripeService stripeService;

    public AudioController(FileService fileService, AudioService audioService, JobArtifactRepository jobArtifactRepository, TranscriptionJobRepository transcriptionJobRepository, AudioFileRepository audioFileRepository, UserService userService, StripeService stripeService) {
        this.fileService = fileService;
        this.audioService = audioService;
        this.jobArtifactRepository = jobArtifactRepository;
        this.transcriptionJobRepository = transcriptionJobRepository;
        this.audioFileRepository = audioFileRepository;
        this.userService = userService;
        this.stripeService = stripeService;
    }


    @RequiresSubscription
    @RequiresStorageQuota
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CompletableFuture<ResponseEntity<Map<String, Object>>> upload(
            @RequestParam("file") MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            return CompletableFuture.completedFuture( ResponseEntity.badRequest()
                    .body(Map.of("error", "file is required"))
            );
        }
        String contentType = file.getContentType();
        if(file.isEmpty()){
            return  CompletableFuture.completedFuture(ResponseEntity.badRequest().body(Map.of("error", "No file sent.")));
        }
        if(file.getOriginalFilename()== null){
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(Map.of("error", "File Name cannot be empty.")));
        }
        if (contentType == null || !contentType.startsWith("audio/")) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(Map.of("error", "invalid content type.")));
        }


        try {
            //create directory if needed
            Path audioDir = fileService.getAUDIO_DIR().toAbsolutePath().normalize();
            Files.createDirectories(audioDir);


            String originalName = file.getOriginalFilename();
            String safeName = originalName == null
                    ? ""
                    : Paths.get(originalName).getFileName().toString();

            String ext = fileService.getExtension(safeName);
            if (ext == null) {
                ext = fileService.inferExtension(file.getContentType());
            }

            if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
                return CompletableFuture.completedFuture(
                        ResponseEntity.badRequest().body(Map.of(
                        "error", "file extension not supported",
                        "allowed", ALLOWED_EXTENSIONS)
                        )
                );
            }

            String fileName = UUID.randomUUID() + "." + ext;
            Path target = audioDir.resolve(fileName).normalize();

            // 🔐 SECURITY CHECK (now works correctly)
            if (!target.startsWith(audioDir)) {
                return CompletableFuture.completedFuture(ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid file path"))
                );
            }

            Files.write(target, file.getBytes(), StandardOpenOption.CREATE_NEW);


            if (!ext.equals("webm")) {
                System.out.println("converting to webm file");
                AppUser user = userService.getCurrentUser();
                Long userId = user.getId();
                String stripeCustomerId = user.getStripeCustomerId();
                return this.audioService.convertToWebM(target).thenApply(x -> {

                    fileService.deleteFile(target);
                    String mp3Name = x.getFileName().toString();

                    long fileSize = 0;

                    try {
                        fileSize = fileService.getFileSize(target); //this is needed to check how much storage the user has used
                    } catch (IOException e) {
                       return ResponseEntity.internalServerError().body(Map.of("error","IO error:"+e.getMessage()));
                    }

                    if(fileSize == 0){
                        return ResponseEntity.badRequest().body(Map.of("error","File size is 0. Cannot save."));
                    }


                    audioService.saveMetadata(file,mp3Name,userId,fileSize);
                    userService.consumeFreeActionIfNeeded();//this reports the free actions used for the free trial

//                    get fileSize
                    AudioFileEntity audioFileEntity =  this.audioFileRepository.findByFileName(fileName).orElse(null);
                    if(audioFileEntity != null){
                        double hours = 0d; //get audio duration length

                        try {
                            hours = fileService.getAudioDurationInHours(target);


                        } catch (IOException e) {
                            return ResponseEntity.internalServerError().body(Map.of("error","IO error:"+e.getMessage()));
                        } catch (InterruptedException e) {
                            return ResponseEntity.internalServerError().body(Map.of("error","Interrupted exception error:"+e.getMessage()));
                        }
                        if(hours != 0.0){
                            audioFileEntity.setDuration(hours * 60.0); //SAVE DURATION OF AUDIO FILE
                        }
                    }
                    return ResponseEntity.ok(Map.of("fileName", mp3Name));//upload went well
                });
            }

            Long userId = userService.getCurrentUser().getId();
            if(userId == null){
                System.out.println("1. audioController/upload: User is null");
                return CompletableFuture.completedFuture(ResponseEntity.internalServerError()
                        .body(Map.of(
                                "error", "user is null"
                        )));
            }

            long fileSize = fileService.getFileSize(target);//this is needed to check how much storage the user has used
            audioService.saveMetadata(file,fileName,userId,fileSize);
            userService.consumeFreeActionIfNeeded();//this reports the free actions used for the free trial

            return CompletableFuture.completedFuture( ResponseEntity.ok(Map.of("fileName",fileName,"message","upload successful")));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return  CompletableFuture.completedFuture(ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Failed to save audio file",
                            "details", e.getMessage()
                    )));
        }
    }

    @PostMapping("/get-one")
    public ResponseEntity<AudioFileDto> getSingleAudioFile(
            @RequestBody Map<String, String> body
    ) throws IOException {

        String fileName = body.get("fileName");

        //check if they own the audio file
        if(!fileService.isOwnerOfAudioFile(fileName,audioFileRepository)){
//            System.out.println(audioFileRepository.findByFileName(fileName));
            return  ResponseEntity.badRequest().build();
        }

        if (fileName == null || fileName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 🔐 sanitize filename only (no directories allowed)
        Path safeName = Paths.get(fileName).getFileName();

        Path audioDir = fileService.getAUDIO_DIR().toAbsolutePath().normalize();
        // 1Try primary AUDIO_DIR (existing behavior)
        Path audioPath = audioDir.resolve(safeName).normalize();

        // Final existence check
        if (!Files.exists(audioPath)) {
            return ResponseEntity.notFound().build();
        }

        Path p = fileService.getAUDIO_DIR()
                .toAbsolutePath()
                .resolve(safeName)
                .normalize();

        TranscriptionJobEntity tjEntity =
                transcriptionJobRepository
                        .findByAudioPath(p.toString())
                        .orElse(null);

        AudioFileDto dto;

        if (tjEntity != null) {

            AudioFileEntity audioFileEntity = audioFileRepository.findByFileName(safeName.toString()).orElse(null);
            String originalFileName = "";
            if (audioFileEntity != null) {
                originalFileName = audioFileEntity.getOriginalFileName();
            }
            dto = new AudioFileDto(
                    tjEntity.getId(),
                    safeName.toString(),
                    Files.size(audioPath),
                    fileService.safePath(tjEntity.getSummaryFileName()),
                    fileService.safePath(tjEntity.getTranscriptFileName()),
                    originalFileName
            );
        } else {
            System.out.println("1. Audio Controller:");
            System.out.println(p.toString());
            System.out.println("Audio File does not exist");
            dto = new AudioFileDto(
                    null,
                    safeName.toString(),
                    Files.size(audioPath),
                    "",
                    "",
                    ""
            );
        }

        return ResponseEntity.ok(dto);
    }


//    @PostMapping("/list")
//    public ResponseEntity<List<AudioFileDto>> listAudioFiles(@RequestBody Map<String,Integer> body) throws IOException {
//
//        //get page and size from request
//        if(body == null){
//            System.out.println("body is null");
//            return ResponseEntity.badRequest().build();
//        }
//
//        if(body.get("page") == null){
//            System.out.println("page is null");
//            return ResponseEntity.badRequest().build();
//        }
//        if(body.get("size") == null){
//            System.out.println("size is null");
//            return ResponseEntity.badRequest().build();
//        }
////        get the user
//        AppUser user = userService.getCurrentUser();
//        if(user == null){
//            return ResponseEntity.badRequest().body(List.of());
//        }
//
//        Pageable pageable = PageRequest.of(body.get("page"), body.get("size"));
////        get the audio directory Path
//        Path audioDir = fileService.getAUDIO_DIR().toAbsolutePath().normalize();
//
////        get a list of all the audio files owned
////        List<AudioFileEntity> audioFilesMetadata = audioFileRepository
////                .findAllByOwnerIdOrderByCreatedAtDesc(user.getId()).orElse(null);
//        List<AudioFileEntity> audioFilesMetadata = audioFileRepository.findAllByOwnerIdOrderByCreatedAtDesc(user.getId(),pageable).orElse(null);
//
//        if(audioFilesMetadata != null){
//            List<AudioFileDto> data = audioFilesMetadata
//                    .stream() //convert it to stream so we can do a map
//                    .map(entity->{
//                        try{
//                            //get the file path
//                            Path filePath = audioDir.resolve(entity.getFileName());
////                            if file path does not exist return null
//                            if(!Files.exists(filePath)){
//                                return null;
//                            }
//                            //get transcriptionJob entity to get the summaryFileName and the transcriptFileName
//                            TranscriptionJobEntity tjEntity = transcriptionJobRepository.findByAudioPath(filePath.toString()).orElse(null);
//                            String summaryFileName = "";
//                            String transcriptionFileName = "";
//                            if(tjEntity != null){
//                                summaryFileName = tjEntity.getSummaryFileName() != null
//                                        ? tjEntity.getSummaryFileName().replace("\\", "/")
//                                        : "";
//
//                                transcriptionFileName = tjEntity.getTranscriptFileName() != null
//                                        ? tjEntity.getTranscriptFileName().replace("\\", "/")
//                                        : "";
//                            }
//                            return new AudioFileDto( //convert it to AudioFileDto to send to the frontend
//                                    tjEntity != null? tjEntity.getId(): null,
//                                    entity.getFileName(),
//                                    Files.size(filePath),
//                                    summaryFileName,
//                                    transcriptionFileName,
//                                    entity.getOriginalFileName());
//                        }
//                        catch (IOException e){
//                            return null;
//                        }
//                    }).filter(Objects::nonNull).toList();
//            return ResponseEntity.ok(data);
//        }
//        return ResponseEntity.badRequest().body(List.of());
//    }

    @PostMapping("/list")
    public ResponseEntity<Page<AudioFileDto>> listAudioFiles(
            @RequestBody Map<String, Integer> body
    ) throws IOException {

        if (body == null || body.get("page") == null || body.get("size") == null) {
            return ResponseEntity.badRequest().build();
        }

        AppUser user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Pageable pageable = PageRequest.of(body.get("page"), body.get("size"));

        Page<AudioFileEntity> audioFilesPage =
                audioFileRepository.findAllByOwnerIdOrderByCreatedAtDesc(
                        user.getId(),
                        pageable
                );

        Path audioDir = fileService.getAUDIO_DIR().toAbsolutePath().normalize();

        Page<AudioFileDto> dtoPage = audioFilesPage.map(entity -> {
            try {
                Path filePath = audioDir.resolve(entity.getFileName());

                long fileSize = Files.exists(filePath)
                        ? Files.size(filePath)
                        : 0L;

                TranscriptionJobEntity tjEntity =
                        transcriptionJobRepository
                                .findByAudioPath(filePath.toString())
                                .orElse(null);

                String summaryFileName = "";
                String transcriptionFileName = "";

                if (tjEntity != null) {
                    summaryFileName = tjEntity.getSummaryFileName() != null
                            ? tjEntity.getSummaryFileName().replace("\\", "/")
                            : "";

                    transcriptionFileName = tjEntity.getTranscriptFileName() != null
                            ? tjEntity.getTranscriptFileName().replace("\\", "/")
                            : "";
                }

                return new AudioFileDto(
                        tjEntity != null ? tjEntity.getId() : null,
                        entity.getFileName(),
                        fileSize,
                        summaryFileName,
                        transcriptionFileName,
                        entity.getOriginalFileName()
                );

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return ResponseEntity.ok(dtoPage);
    }



    @PostMapping(
            value = "/getfile",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Resource> streamAudioPost(
            @RequestBody Map<String, String> body
    ) {
        try{
            String fileName = body.get("fileName");

            if (fileName == null || fileName.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            if(!fileService.isOwnerOfAudioFile(fileName,audioFileRepository)){
                return  ResponseEntity.badRequest().build();
            }

            Path audioDir = fileService.getAUDIO_DIR().toAbsolutePath().normalize();
            // Resolve & normalize
            Path file = audioDir.resolve(fileName).normalize();

//            System.out.println("AUDIO_DIR = " + audioDir);
//            System.out.println("fileName = " + fileName);
//            System.out.println("resolved = " + file);

            // 🔐 Path traversal protection
            if (!file.startsWith(audioDir) || !Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource =
                    new UrlResource(file.toUri());

            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = "audio/mpeg";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

