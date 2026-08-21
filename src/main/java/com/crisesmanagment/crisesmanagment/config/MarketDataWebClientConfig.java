package com.crisesmanagment.crisesmanagment.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class MarketDataWebClientConfig {

    @Bean(name = "eiaWebClient")
    public WebClient eiaWebClient(WebClient.Builder builder) {
        return builder.baseUrl("https://api.eia.gov/v2").build();
    }

    @Bean(name = "gdeltWebClient")
    public WebClient gdeltWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 20000)
                .responseTimeout(Duration.ofSeconds(20));
        return builder
                .baseUrl("https://api.gdeltproject.org/api/v2")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}