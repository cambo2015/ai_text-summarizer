package com.aisummarizer.website.services;



import com.aisummarizer.website.config.MY_PROPERTIES;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static com.aisummarizer.website.helpers.AllowedExtensions.ALLOWED_EXTENSIONS;

@Service
public class YoutubeDownloadService {

    //    @Value("${youtube.output.directory}")
//    public static String outputDirectory = "./audio";
//    public String videoOutputDirectory = "./video";
     private  String outputDirectory;
     private MY_PROPERTIES myProperties;

     public YoutubeDownloadService( MY_PROPERTIES myProperties) {
         this.myProperties = myProperties;
         this.outputDirectory = myProperties.getAudioDir().toString();
     }

    @Async("ytDlpExecutor")
    public CompletableFuture<String> downloadYoutubeVideo(String youtubeUrl){


//        output directory
        try {
            String uuid = UUID.randomUUID().toString();

            List<String> command = new ArrayList<>();
            command.add("yt-dlp");
            command.add("-x");
            command.add("--audio-format");
            command.add("mp3");
            command.add("-P");
//            command.add(YoutubeDownloadService.outputDirectory);
            command.add(this.outputDirectory);
            command.add(youtubeUrl);
            command.add("-o");
            command.add(uuid + ".%(ext)s");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            //get each line in the reader
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if(exitCode != 0){
                throw new RuntimeException("yt-dlp-failed with exit code"+exitCode);
            }
//            return CompletableFuture.completedFuture(output.toString());
            return CompletableFuture.completedFuture("Audio downloaded successfully");
        }
        catch(IOException | InterruptedException | RuntimeException e){
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("ytDlpExecutor")
    public CompletableFuture<String> extractAudioFromVideoFile(Path videoFileLocation){

//        output directory
        try {
            List<String> command = new ArrayList<>();
            command.add("yt-dlp");
            command.add("-x");
            command.add("--audio-format");
            command.add("mp3");
            command.add("-P");
//            command.add(YoutubeDownloadService.outputDirectory);
            command.add(this.outputDirectory);
            command.add(videoFileLocation.toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            //get each line in the reader
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if(exitCode != 0){
                throw new RuntimeException("yt-dlp-failed with exit code"+exitCode);
            }
            return CompletableFuture.completedFuture("Audio downloaded successfully");
        }
        catch(IOException | InterruptedException | RuntimeException e){
            return CompletableFuture.failedFuture(e);
        }
    }
}
