package com.aisummarizer.website.services;

import com.aisummarizer.website.entities.WhisperType;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

/**
 * Executes Whisper CLI to extract text from audio files.
 *
 * <p>
 * Whisper always names output files based on the audio filename.
 * This class normalizes the result by renaming the transcript to:
 *
 * <pre>
 *   ./transcripts/{jobId}.txt
 * </pre>
 *
 * This guarantees a stable contract for downstream job processing.
 * </p>
 */
@Service
public class WhisperTextExtractor {



    private final Path TRANSCRIPT_DIR;
    FileService fileService;
    private final OpenAiAudioTranscriptionModel openAi;
    private final FileEncrypterDecrypterService fileEncryptDecryptService;


    public WhisperTextExtractor(FileService fileService, @Qualifier("openAiAudioTranscriptionModel")OpenAiAudioTranscriptionModel openAi,FileEncrypterDecrypterService fileEncryptDecrypterService) {
        this.fileService = fileService;
        this.openAi = openAi;
        this.TRANSCRIPT_DIR = fileService.getTRANSCRIPTS_DIR();
        this.fileEncryptDecryptService = fileEncryptDecrypterService;
    }

    public Path extract(Path audioFile, UUID jobId, WhisperType whisperType) throws Exception {

        if(whisperType == WhisperType.INTERNET) {
            return extractOnline(audioFile,jobId);
        }else{
            return extractLocally(audioFile,jobId);
        }
    }

    /**
     * Runs Whisper on the given audio file and returns the normalized transcript path.
     *
     * @param audioFile audio file to transcribe
     * @param jobId     job identifier used for output filename
     * @return path to ./transcripts/{jobId}.txt
     */
    public Path extractLocally(Path audioFile, UUID jobId) throws Exception {
        // Ensure output directory exists
        Files.createDirectories(this.TRANSCRIPT_DIR);

        // Build Whisper command
        List<String> command = List.of(
                "whisper",
                audioFile.toAbsolutePath().toString(),
                "--model", "small",
                "--output_format", "txt",
                "--output_dir", this.TRANSCRIPT_DIR.toString(),
                "--verbose", "False"
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Stream Whisper output (useful for debugging)
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[whisper] " + line);
            }
        }

        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Whisper failed with exit code " + exit);
        }

        // Whisper-generated filename (based on audio name)
        String whisperFileName = audioFile.getFileName()
                .toString()
                .replaceFirst("\\.[^.]+$", ".txt");

        Path whisperOutput = this.TRANSCRIPT_DIR.resolve(whisperFileName);

        if (!Files.exists(whisperOutput)) {
            throw new IllegalStateException(
                    "Whisper output file not found: " + whisperOutput);
        }

        // Normalize to jobId.txt
        Path finalOutput = this.TRANSCRIPT_DIR.resolve(jobId + ".txt");

        Files.move(
                whisperOutput,
                finalOutput,
                StandardCopyOption.REPLACE_EXISTING
        );
        return finalOutput;
    }

    public Path extractOnline(Path audioFile, UUID jobId) throws Exception {
//        set response format
        OpenAiAudioApi.TranscriptResponseFormat responseFormat = OpenAiAudioApi.TranscriptResponseFormat.TEXT;
        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .language("en")
                .responseFormat(responseFormat)
                .build();

        Resource audioResource = new FileSystemResource(audioFile);
        AudioTranscriptionPrompt  transcriptRequest = new AudioTranscriptionPrompt(audioResource,transcriptionOptions);
        AudioTranscriptionResponse response = this.openAi.call(transcriptRequest);
        String text =  response.getResult().getOutput();

        //encrypt the data
        String encrypted = fileEncryptDecryptService.encrypt(text);
        //save the text
        return fileService.saveTranscriptionFile(jobId, encrypted);
    }

}
