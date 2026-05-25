package com.aisummarizer.website.controllers;


import com.aisummarizer.website.aspects.RequiresSubscription;
import com.aisummarizer.website.services.AudioService;
import com.aisummarizer.website.services.FileService;
import com.aisummarizer.website.services.UserService;
import com.aisummarizer.website.services.YoutubeDownloadService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.aisummarizer.website.helpers.AllowedExtensions.ALLOWED_EXTENSIONS;


@RestController
@RequestMapping("/api/youtube")
@CrossOrigin("http://localhost:4200")
@RequiresSubscription
public class YoutubeController {

    private final FileService fileService;
    public YoutubeDownloadService ytService;
    public AudioService audioService;
    public final UserService userService;
    public YoutubeController(YoutubeDownloadService yts, FileService fileService, AudioService audioService, UserService userService) {
        this.ytService = yts;
        this.fileService = fileService;
        this.audioService = audioService;
        this.userService = userService;
    }

//    @PostMapping("/download-audio")
//    public DeferredResult<ResponseEntity<String>> downloadAudioFile(
//            @RequestBody YoutubeRequest request) {
//
//        DeferredResult<ResponseEntity<String>> deferred = new DeferredResult<>();
//
//        String youtubeUrl = request.getYoutubeUrl().trim();
//
//        if (youtubeUrl.isEmpty()) {
//            deferred.setResult(
//                    ResponseEntity.badRequest().body("You must provide a YouTube URL")
//            );
//            return deferred;
//        }
//
//        ytService.downloadYoutubeVideo(youtubeUrl)
//                .thenAccept(result ->
//                        deferred.setResult(ResponseEntity.ok(result)))
//                .exceptionally(ex -> {
//                    deferred.setErrorResult(
//                            ResponseEntity.status(500).body(ex.getMessage()));
//                    return null;
//                });
//
//        return deferred;
//    }


//    @PostMapping("/upload")
//    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
//
//        String contentType = file.getContentType();
//        if(file.isEmpty()){
//            return  ResponseEntity.badRequest().body("No file was sent in the request.");
//        }
//        if(file.getOriginalFilename()== null){
//            return ResponseEntity.badRequest().body("fileName cannot be null.");
//        }
//        if (contentType == null || !contentType.startsWith("audio/")) {
//            return ResponseEntity.badRequest().body("Invalid content type");
//        }
//
//        String originalFileName = Paths
//                .get(file.getOriginalFilename())
//                .getFileName()
//                .toString();
//        String ext = fileService.getExtension(originalFileName);
//
//        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
//            return ResponseEntity.badRequest().body("That extension is not supported");
//        }
//
//
//        Long userId = userService.getCurrentUser().getId();
//        UUID fileName = UUID.randomUUID();
//
//        String newFileName = fileName +  "." + ext;
//
//        //saves the audioFile
//        boolean savedSuccessfully = fileService.saveAudioFile(file,newFileName);
//
//        //save audioFileMetadata
//        audioService.saveMetadata(file,newFileName,userId);
//        userService.consumeFreeActionIfNeeded();
//        if (savedSuccessfully) {
//            return ResponseEntity.ok("Successfully saved");
//        }
//        return ResponseEntity.badRequest().body("Failed to save file");
//    }
}

