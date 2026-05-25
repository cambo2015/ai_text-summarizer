package com.aisummarizer.website.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Getter
@Setter
public class MY_PROPERTIES {

    private Path storageRoot;
    private Path audioDir;
    private Path llmInstructionsDir;
    private Path pythonDir;
    private Path transcriptsDir;
    private Path summaryDir;
    public MY_PROPERTIES(
            @Value("${app.storage.root}") String storageRoot
    ) {

        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
        this.audioDir = this.storageRoot.resolve("audio");
        this.transcriptsDir = this.storageRoot.resolve("transcripts");
        this.summaryDir = this.storageRoot.resolve("summary");
        this.llmInstructionsDir = this.storageRoot.resolve("llm-instructions");
        this.pythonDir = this.storageRoot.resolve("python");

//        System.out.println("MY_PROPERTIES storageRoot raw = [" + storageRoot + "]");
//        System.out.println("MY_PROPERTIES user.dir = [" + System.getProperty("user.dir") + "]");


        if (isLikelyDocker() && !"/files".equals(storageRoot)) {
            throw new IllegalStateException("In Docker but STORAGE_ROOT is not /files. Got: " + storageRoot);
        }
    }
    private boolean isLikelyDocker() {
        return java.nio.file.Files.exists(java.nio.file.Path.of("/.dockerenv"));
    }
}
