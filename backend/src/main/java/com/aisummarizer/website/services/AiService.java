package com.aisummarizer.website.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Executes LLM-based summarization using the `llm` CLI.
 */
@Service
public class AiService {

    private final FileService fileService;
    private final ChatClient openAi;
    private final ChatClient mistral;
    private final FileEncrypterDecrypterService  fileEncrypterDecrypterService;

    private final String systemString = "You are a summarizer that summarizes meetings.";
    public AiService(FileService fileService, @Qualifier("openAiChat") ChatClient openAi, @Qualifier("mistralChat") ChatClient mistral,FileEncrypterDecrypterService fileEncrypterDecrypterService) {
        this.fileService = fileService;
        this.openAi = openAi;
        this.mistral = mistral;
        this.fileEncrypterDecrypterService = fileEncrypterDecrypterService;
    }

    public String getText(Model provider,String prompt,String model){
        return switch(provider){
            case MISTRAL -> askMistral(prompt,model);
            case CHAT_GPT-> askOpenAi(prompt,model);
            default -> askMistral(prompt,model);
        };
    }

    public String askMistral(String prompt,String model){

        String instructions = "";
        if(fileService.llmCustomFileExists()){
            instructions = fileService.getLLMCustomInstructions();
            MistralAiChatOptions options = MistralAiChatOptions
                    .builder()
                    .model(model)
                    .build();

            return mistral
                    .prompt()
                    .options(options)
                    .system(systemString)
                    .user(instructions+" "+prompt)
                    .call()
                    .content();
        }

        return mistral
                .prompt()
                .system(systemString)
                .user("Make a summary of this text but split it up into different sections with section headers and bullet points. This is a meeting summary of all our notes. Lastly make a overall summary so we know the context and topic of the meeting:"+prompt)
                .call()
                .content();
    }

    public String askOpenAi(String prompt,String model){
        String instructions = "";
        OpenAiChatOptions options = OpenAiChatOptions
                .builder()
                .model(model)
                .temperature(1.0)
                .build();
        if(fileService.llmCustomFileExists()){
            instructions = fileService.getLLMCustomInstructions();

            return openAi
                    .prompt()
                    .options(options)
                    .system(systemString)
                    .user(instructions+" "+prompt)
                    .call()
                    .content();
        }

        return openAi
                .prompt()
                .options(options)
                .system(systemString)
                .user("Make a summary of this text but split it up into different sections with section headers and bullet points. This is a meeting summary of all our notes. Lastly, make a overall summary so we know the context and topic of the meeting."+prompt)
                .call()
                .content();
    }

    public Path summarize(String transcriptText,UUID jobId,String model){
        //convert string to Model
        Model m = convertStringToModel(model);
        String s = getText(m,transcriptText,model);
        System.out.println(s);
        if(s != null){
            String encryptedText =  fileEncrypterDecrypterService.encrypt(s);
            return fileService.saveSummary(jobId,encryptedText);
        }
        else{
            System.out.println("summarization script is null!");
            return null;
        }
        //save to file

    }


    /**
     * <p>converts a string to a Model enum</p>
     * @param model
     * @return Model
     */
    public Model convertStringToModel(String model){
        model = model.toLowerCase();
        if(model.contains("mistral")){
            return Model.MISTRAL;
        }
        else if(model.contains("gpt") || model.contains("o1") || model.contains("o2") || model.contains("o3") || model.contains("o4")){
            return Model.CHAT_GPT;
        }
        return Model.MISTRAL;
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
            System.out.println("[LLM] Starting summarization job " + jobId);
//            System.out.println("[LLM] Executable exists: " +
//                    new java.io.File(LLM_PATH).exists());

            ProcessBuilder pb = new ProcessBuilder(
//                    LLM_PATH,
                    "llm",
                    "summarize",
                    "--model", "mistral-small"
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

//    do not use. This is deprecated and dangerous
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
                System.out.println("[LLM] Custom file exists. Now we are starting to get the summary");
                customFileText = fileService.getLLMCustomInstructions();
                pb = new ProcessBuilder(
                        "llm",
                        "--model",
//                        "mistral-small"
                        model
                );

            }else{
                //proceed to summarize normally
                pb = new ProcessBuilder(
                        "llm",
                        "summarize",
                        "--model",
//                        "mistral-small"
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
            output = output.replace("#","").replace("*","");

            Path saved = fileService.saveSummary(jobId, output);
            System.out.println("[LLM] Summary saved to " + saved);

            return CompletableFuture.completedFuture(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

}
