package com.crisesmanagment.crisesmanagment.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class MarketDataWebClientConfig {

    @Value("${weather.api.base-url:https://api.open-meteo.com}")
    private String weatherApiBaseUrl;


    @Bean(name = "eiaWebClient")
    public WebClient eiaWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.eia.gov/v2")
                .build();
    }


    @Bean(name = "gdeltWebClient")
    public WebClient gdeltWebClient(WebClient.Builder builder) {

        HttpClient httpClient = HttpClient.create()

                // Time allowed to establish TCP connection
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        30000
                )

                // Maximum time waiting for the HTTP response
                .responseTimeout(
                        Duration.ofSeconds(60)
                );

        return builder
                .baseUrl("https://api.gdeltproject.org/api/v2")
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient)
                )
                .build();
    }


    @Bean(name = "weatherWebClient")
    public WebClient weatherWebClient(WebClient.Builder builder) {

        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        10000
                )
                .responseTimeout(
                        Duration.ofSeconds(10)
                );

        return builder
                .baseUrl(weatherApiBaseUrl)
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient)
                )
                .build();
    }
}