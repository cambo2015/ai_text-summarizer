//package com.aisummarizer.website.components;
//
//
//import com.aisummarizer.website.dto.JobType;
//import com.aisummarizer.website.dto.JobWorker;
//import com.aisummarizer.website.services.TextExtractorService;
//import org.springframework.stereotype.Component;
//
//
//import java.util.UUID;
//
//@Component
//public class WhisperJobWorker implements JobWorker {
//
//    private final TextExtractorService textExtractorService;
//
//    public WhisperJobWorker(TextExtractorService textExtractorService) {
//        this.textExtractorService = textExtractorService;
//    }
//
//    @Override
//    public JobType getType() {
//        return JobType.WHISPER;
//    }
//
//    @Override
//    public void start(UUID jobId) {
//        // Delegate to existing logic
//        textExtractorService.processJob(jobId);
//    }
//}
//
//
