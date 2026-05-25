package com.aisummarizer.website.dto;


import java.util.UUID;

public interface JobWorker {

    JobType getType();

    void start(UUID jobId);
}


