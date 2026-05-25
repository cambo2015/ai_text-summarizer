package com.aisummarizer.website.jobs;


import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TextJobStore {
    private final Map<String, TextExtractionJob> jobs = new ConcurrentHashMap<>();

    public TextExtractionJob createJob() {
        String id = UUID.randomUUID().toString();
        TextExtractionJob job = jobs.get(id);
        jobs.put(id, job);
        return job;
    }

    public TextExtractionJob getJob(String id) {
        return jobs.get(id);
    }
}
