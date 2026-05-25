package com.aisummarizer.website.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "ytDlpExecutor")
    public Executor ytDlpExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("yt-dlp-");
        executor.initialize();
        return executor;
    }

    // CPU-bound: Whisper transcription
    @Bean(name = "whisperExecutor")
    public Executor whisperExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("whisper-");
        executor.initialize();
        return new DelegatingSecurityContextExecutor(executor);
    }

    @Bean(name = "llmExecutor")
    public Executor llmExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("llm-");
        executor.initialize();
        return new DelegatingSecurityContextExecutor(executor);
    }

    @Bean(name = "commonWordExecutor")
    public ThreadPoolTaskExecutor CommonWordExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);      // safe default
        executor.setMaxPoolSize(4);       // limit concurrent Python processes
        executor.setQueueCapacity(10);    // 👈 important: fail fast
        executor.setThreadNamePrefix("llm-");

        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.AbortPolicy()
        );

        executor.initialize();
        return executor;
    }

    @Bean(name = "ffmpegExecutor")
    public Executor ffmpegExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("ffmpeg-");
        executor.initialize();
        return executor;
    }

}
