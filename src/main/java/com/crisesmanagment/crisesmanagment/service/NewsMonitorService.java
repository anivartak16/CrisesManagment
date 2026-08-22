package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RiskEventRequestDto;
import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsMonitorService {

    @Qualifier("gdeltWebClient")
    private final WebClient gdeltWebClient;

    private final GeminiExtractionService geminiExtractionService;
    private final RouteRepository routeRepository;
    private final RiskEventRepository riskEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Gap between per-route GDELT requests within one polling pass. GDELT's
    // free doc/doc endpoint has no published quota, but empirically starts
    // returning 429s / dropping connections once you're a few requests deep
    // in quick succession — 8s keeps a 6-route sweep well under that.
    private static final Duration GDELT_REQUEST_SPACING = Duration.ofSeconds(8);


    @Scheduled(fixedRate = 900000)
    public void pollShippingNews() {

        log.info("Starting GDELT shipping news polling...");

        List<Route> routes = routeRepository.findAll();

        for (int i = 0; i < routes.size(); i++) {

            Route route = routes.get(i);

            try {

                pollNewsForRoute(route);

            } catch (Exception e) {

                log.error(
                        "Unexpected error while polling news for route {}",
                        route.getName(),
                        e
                );
            }

            // GDELT's public doc/doc endpoint rate-limits aggressively per
            // client. Firing one request per route back-to-back (6 routes =
            // 6 requests in a couple of seconds) reliably triggers 429s and
            // dropped connections for everything after the first call. A
            // short pause between routes keeps us under that limit. Skipped
            // after the last route so we don't delay the "finished" log for
            // no reason.
            if (i < routes.size() - 1) {
                try {
                    Thread.sleep(GDELT_REQUEST_SPACING.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("GDELT polling loop interrupted while spacing out requests; stopping this run.");
                    break;
                }
            }
        }

        log.info("Finished GDELT shipping news polling.");
    }


    private void pollNewsForRoute(Route route) {

        String query = route.getName() + " oil shipping disruption";

        log.info(
                "Fetching GDELT news for route: {}",
                route.getName()
        );


        String response = gdeltWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/doc/doc")
                        .queryParam("query", query)
                        .queryParam("mode", "artlist")
                        .queryParam("maxrecords", 3)
                        .queryParam("timespan", "1d")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(String.class)

                // Maximum time allowed for this request
                .timeout(Duration.ofSeconds(45))

                // Retry only network/timeout-related failures
                .retryWhen(
                        Retry.backoff(2, Duration.ofSeconds(3))
                                .maxBackoff(Duration.ofSeconds(15))
                                .filter(this::isRetryableError)
                                .doBeforeRetry(signal ->
                                        log.warn(
                                                "Retrying GDELT request for route {}. Attempt: {}. Error: {}",
                                                route.getName(),
                                                signal.totalRetries() + 1,
                                                signal.failure().getMessage()
                                        )
                                )
                )

                // Prevent application failure if GDELT is unavailable
                .onErrorResume(e -> {

                    log.error(
                            "GDELT request failed for route {} after retries. Error: {}",
                            route.getName(),
                            e.getMessage()
                    );

                    return Mono.empty();
                })

                // Prevent scheduled thread from waiting forever
                .block(Duration.ofSeconds(60));


        if (response == null || response.isBlank()) {

            log.warn(
                    "No response from GDELT for route {}",
                    route.getName()
            );

            return;
        }


        try {

            JsonNode articles = objectMapper
                    .readTree(response)
                    .path("articles");


            if (!articles.isArray()) {

                log.warn(
                        "No articles found in GDELT response for route {}",
                        route.getName()
                );

                return;
            }


            // Load existing events ONCE instead of querying
            // the database for every article.
            List<RiskEvent> existingEvents =
                    riskEventRepository.findByRouteId(route.getId());

            // Track headlines already seen for this route (from the DB,
            // plus any processed earlier in this same batch) so that two
            // near-identical articles in one GDELT response don't both
            // get turned into separate risk events.
            Set<String> seenHeadlines = new HashSet<>();
            for (RiskEvent existing : existingEvents) {
                if (existing.getRawText() != null) {
                    seenHeadlines.add(existing.getRawText().toLowerCase());
                }
            }


            for (JsonNode article : articles) {

                String headline =
                        article.path("title").asText("").trim();


                if (headline.isBlank()) {
                    continue;
                }


                boolean alreadyExists =
                        seenHeadlines.contains(headline.toLowerCase());


                if (alreadyExists) {

                    log.debug(
                            "Skipping duplicate headline for route {}: {}",
                            route.getName(),
                            headline
                    );

                    continue;
                }


                log.info(
                        "Processing new GDELT headline for route {}: {}",
                        route.getName(),
                        headline
                );


                RiskEventRequestDto dto =
                        new RiskEventRequestDto();

                dto.setRawText(headline);
                dto.setRouteId(route.getId());


                try {

                    geminiExtractionService.extractAndSave(dto);
                    seenHeadlines.add(headline.toLowerCase());

                    log.info(
                            "Auto-created risk event for route {} from headline: {}",
                            route.getName(),
                            headline
                    );

                } catch (Exception e) {

                    log.error(
                            "Failed to process headline for route {}: {}",
                            route.getName(),
                            headline,
                            e
                    );
                }
            }

        } catch (Exception e) {

            log.error(
                    "Failed to parse GDELT response for route {}",
                    route.getName(),
                    e
            );
        }
    }


    /**
     * Only retry temporary network-related problems.
     */
    private boolean isRetryableError(Throwable error) {

        Throwable current = error;

        while (current != null) {

            if (current instanceof ConnectException ||
                    current instanceof TimeoutException ||
                    current instanceof io.netty.handler.ssl.SslHandshakeTimeoutException) {

                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}