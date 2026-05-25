package com.aisummarizer.website.jobs;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
public class TextExtractionJob {
    private String id;
    private JobStatus status;
    private Path transcriptPath;
    private String error;

    public TextExtractionJob(String id, JobStatus status, Path transcriptPath, String error) {
        this.id = id;
        this.status = status;
        this.transcriptPath = transcriptPath;
        this.error = error;
    }
}
