package com.aisummarizer.website.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class AiChatConfig {

    @Bean
    @Qualifier("openAiChat")
    ChatClient openAiChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model).build();
    }

    @Bean
    @Qualifier("mistralChat")
    ChatClient mistralChatClient(MistralAiChatModel model) {
        return ChatClient.builder(model).build();
    }
}
