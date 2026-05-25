package com.aisummarizer.website.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Executes LLM-based summarization using the `llm` CLI.
 */
@Service
public class ChatGptAIService {

//    private static final String LLM_PATH =
//            "C:/Users/camer/AppData/Local/Programs/Python/Python311/Scripts/llm";

    private final FileService fileService;

    public ChatGptAIService(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Summarizes transcript text asynchronously.
     */
    @Async("llmExecutor")
    public CompletableFuture<Path> summarizeAsync(
            String transcriptText,
            UUID jobId
    ) {

        try {
            System.out.println("[LLM] Starting chat gpt normal summarization job " + jobId);

            ProcessBuilder pb = new ProcessBuilder(
//                    LLM_PATH,
                    "llm",
                    "summarize",
                    "--model", "gpt-5"
            );

            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Write transcript to stdin
            try (BufferedWriter writer =
                         new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(transcriptText);
                writer.flush();
            }

            // Read output
            String output;
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            System.out.println("[LLM] Exit code = " + exitCode);

            if (exitCode != 0) {
                throw new RuntimeException("llm failed:\n" + output);
            }

            Path saved = fileService.saveSummary(jobId, output);
            System.out.println("[LLM] Summary saved to " + saved);

            return CompletableFuture.completedFuture(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("llmExecutor")
    public CompletableFuture<Path> customSummarizeAsync(
            String transcriptText,
            UUID jobId,
            String model
    ) {

        try {
            System.out.println("[LLM] Starting chat gpt custom summarization job " + jobId);

            ProcessBuilder pb = new ProcessBuilder();
            String customFileText = "";
            if(fileService.llmCustomFileExists()){
                customFileText = fileService.getLLMCustomInstructions();
                pb = new ProcessBuilder(
                        "llm",
                        "--model",
//                        "gpt-5"
                        model
                );

            }else{
                //proceed to summarize normally
                pb = new ProcessBuilder(
                        "llm",
                        "summarize",
                        "--model",
//                        "gpt-5"
                        model
                );
            }


            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Write transcript to stdin
            try (BufferedWriter writer =
                         new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(customFileText+transcriptText);
                writer.flush();
            }

            // Read output
            String output;
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            System.out.println("[LLM] Exit code = " + exitCode);

            if (exitCode != 0) {
                throw new RuntimeException("llm failed:\n" + output);
            }

            Path saved = fileService.saveSummary(jobId, output);
            System.out.println("[LLM] Summary saved to " + saved);

            return CompletableFuture.completedFuture(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }
}
