package com.crisesmanagment.crisesmanagment.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    // Groq key — when set, GeminiExtractionService calls Gemini AND Groq for
    // each headline and reconciles the two answers instead of trusting a
    // single response. Leave GROQ_API_KEY unset to skip this entirely (this
    // bean is still created, just with a blank auth header, and
    // GeminiExtractionService never calls it in that case).
    @Value("${groq.api.key:}")
    private String groqApiKey;


    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }


    @Bean
    @Qualifier("geminiWebClient")
    public WebClient geminiWebClient(WebClient.Builder builder) {

        return builder
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("x-goog-api-key", geminiApiKey)
                .build();
    }


    @Bean
    @Qualifier("groqWebClient")
    public WebClient groqWebClient(WebClient.Builder builder) {

        return builder
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + groqApiKey)
                .build();
    }
}