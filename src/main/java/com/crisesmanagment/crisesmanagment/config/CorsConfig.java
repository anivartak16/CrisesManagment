package com.crisesmanagment.crisesmanagment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    // The deployed frontend's origin (e.g. https://your-frontend.up.railway.app)
    // — set via the FRONTEND_URL env var in production. Local dev origins
    // stay allowed unconditionally so `npm run dev` keeps working without
    // any env var set.
    @Value("${frontend.url:}")
    private String frontendUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                var mapping = registry.addMapping("/api/**")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");

                if (frontendUrl != null && !frontendUrl.isBlank()) {
                    mapping.allowedOrigins("http://localhost:5173", "http://localhost:3000", frontendUrl);
                } else {
                    mapping.allowedOrigins("http://localhost:5173", "http://localhost:3000");
                }
            }
        };
    }
}