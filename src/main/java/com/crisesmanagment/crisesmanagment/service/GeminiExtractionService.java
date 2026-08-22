package com.crisesmanagment.crisesmanagment.service;

import com.crisesmanagment.crisesmanagment.dto.RiskEventRequestDto;
import com.crisesmanagment.crisesmanagment.dto.RiskEventResponseDto;
import com.crisesmanagment.crisesmanagment.model.Route;
import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import com.crisesmanagment.crisesmanagment.repo.RiskEventRepository;
import com.crisesmanagment.crisesmanagment.repo.RouteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GeminiExtractionService {

    private final WebClient geminiWebClient;
    private final WebClient groqWebClient;
    private final RiskEventRepository riskEventRepository;
    private final RouteRepository routeRepository;
    private final RouteRiskService routeRiskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // The Gemini model to call. Previously this was hardcoded to
    // "gemini-2.5-flash", which Google has since retired (404 NOT_FOUND) —
    // now driven by config so a model bump never requires a code change again.
    @Value("${gemini.api.model:gemini-3.6-flash}")
    private String geminiModel;

    // Groq model used for the cross-check call (see WebClientConfig#groqWebClient).
    @Value("${groq.api.model:openai/gpt-oss-120b}")
    private String groqModel;

    // Groq key. Read here purely to decide *whether* to make the second
    // call at all — calling out with a blank key would just burn a request
    // for a guaranteed 401.
    @Value("${groq.api.key:}")
    private String groqApiKey;

    // How many of the most recent risk events to feed back into the prompt
    // as calibration context, so severity/eventType judgements stay
    // consistent with what's already been logged instead of each headline
    // being scored in isolation.
    private static final int HISTORY_CONTEXT_SIZE = 8;

    public GeminiExtractionService(@Qualifier("geminiWebClient") WebClient geminiWebClient,
                                   @Qualifier("groqWebClient") WebClient groqWebClient,
                                   RiskEventRepository riskEventRepository,
                                   RouteRepository routeRepository,
                                   RouteRiskService routeRiskService) {
        this.geminiWebClient = geminiWebClient;
        this.groqWebClient = groqWebClient;
        this.riskEventRepository = riskEventRepository;
        this.routeRepository = routeRepository;
        this.routeRiskService = routeRiskService;
    }

    public RiskEventResponseDto extractAndSave(RiskEventRequestDto request) {
        String rawText = request.getRawText();

        // Build the list of known routes so Gemini can pick the one the
        // headline is actually about instead of requiring the operator to
        // tag it by hand every time.
        List<Route> allRoutes = routeRepository.findAll();
        String routeList = allRoutes.isEmpty()
                ? "(no routes on file)"
                : allRoutes.stream().map(Route::getName).reduce((a, b) -> a + ", " + b).orElse("");

        String prompt = buildPrompt(rawText, routeList);

        boolean secondaryConfigured = groqApiKey != null && !groqApiKey.isBlank();

        String primaryResponseBody = callGemini(geminiWebClient, prompt, "primary");
        ParsedExtraction primary = parse(primaryResponseBody);

        ParsedExtraction merged;
        ParsedExtraction secondary = null;
        String secondaryResponseBody = null;
        if (secondaryConfigured) {
            secondaryResponseBody = callGroq(groqWebClient, prompt, "secondary");
            secondary = parseGroq(secondaryResponseBody);
            merged = reconcile(primary, secondary, allRoutes);
        } else {
            merged = primary;
        }

        // Persist a structured comparison (when a second key is configured) so
        // the frontend can show primary vs. secondary vs. the reconciled
        // result without having to parse Gemini's raw response envelope
        // itself. Falls back to the plain single-key response otherwise.
        String storedExtractedJson = secondaryConfigured
                ? combineRawResponses(primary, secondary, merged, primaryResponseBody, secondaryResponseBody)
                : primaryResponseBody;

        // Route resolution priority: explicit routeId from the request wins
        // (manual tagging still overrides), otherwise fall back to whatever
        // route the reconciled result matched against the headline by name.
        Route route = null;
        boolean autoDetectedRoute = false;
        if (request.getRouteId() != null) {
            route = routeRepository.findById(request.getRouteId()).orElse(null);
        } else if (merged.routeName != null && !merged.routeName.isBlank()
                && !"null".equalsIgnoreCase(merged.routeName.trim())) {
            String needle = merged.routeName.trim();
            route = allRoutes.stream()
                    .filter(r -> r.getName().equalsIgnoreCase(needle) || r.getName().contains(needle) || needle.contains(r.getName()))
                    .findFirst()
                    .orElse(null);
            autoDetectedRoute = route != null;
        }

        RiskEvent event = RiskEvent.builder()
                .source(secondaryConfigured ? "gemini+groq (dual-model)" : "gemini")
                .route(route)
                .eventType(request.getEventType() != null ? request.getEventType()
                        : (merged.eventType != null && !merged.eventType.isBlank() ? merged.eventType : "UNCLASSIFIED"))
                .severity(request.getSeverity() != null ? request.getSeverity()
                        : (merged.severity != null ? merged.severity : 0))
                .durationDays(request.getDurationDays() != null ? request.getDurationDays() : merged.durationDays)
                .rawText(rawText)
                .extractedJson(storedExtractedJson)
                .build();
        RiskEvent saved = riskEventRepository.save(event);

        // Immediately recompute the affected route's live risk/cost — this is
        // what makes the routes page real-time instead of waiting for the poller.
        if (route != null) {
            routeRiskService.recomputeRouteRisk(route.getId());
        }

        RiskEventResponseDto dto = new RiskEventResponseDto();
        dto.setId(saved.getId());
        dto.setExtractedJson(storedExtractedJson == null ? "" : storedExtractedJson);
        dto.setSeverity(saved.getSeverity());
        dto.setEventType(saved.getEventType());
        dto.setDurationDays(saved.getDurationDays());
        dto.setRouteId(route != null ? route.getId() : null);
        dto.setRouteName(route != null ? route.getName() : null);
        dto.setAutoDetectedRoute(autoDetectedRoute);
        return dto;
    }

    /**
     * Builds the extraction prompt, folding in a compact summary of the most
     * recently logged events so Gemini calibrates severity/eventType against
     * what's already on record instead of scoring each headline in a vacuum.
     */
    private String buildPrompt(String rawText, String routeList) {
        String historyContext = recentHistoryContext();

        return "Analyze this crude oil supply-chain news headline and respond with ONLY "
                + "raw JSON, no markdown, no explanation, in exactly this shape: "
                + "{\"severity\": <integer 0-10>, \"eventType\": \"<CLOSURE|SANCTIONS|ATTACK|WEATHER|CONGESTION|OTHER>\", "
                + "\"durationDays\": <integer>, \"routeName\": \"<one of: " + routeList + ", or null if none clearly match>\"}. "
                + (historyContext.isEmpty() ? "" : "For calibration, here are the most recently logged events "
                + "(most recent first) — keep new severity ratings consistent with these: "
                + historyContext + " ")
                + "Headline: \"" + rawText + "\"";
    }

    private String recentHistoryContext() {
        List<RiskEvent> recent = riskEventRepository.findAllByOrderByCreatedAtDesc();
        if (recent.isEmpty()) {
            return "";
        }
        return recent.stream()
                .limit(HISTORY_CONTEXT_SIZE)
                .map(e -> String.format(
                        "[%s | severity %s | %s | \"%s\"]",
                        e.getEventType() != null ? e.getEventType() : "UNCLASSIFIED",
                        e.getSeverity() != null ? e.getSeverity() : "?",
                        e.getRoute() != null ? e.getRoute().getName() : "unmatched",
                        e.getRawText() != null && e.getRawText().length() > 80
                                ? e.getRawText().substring(0, 80) + "…"
                                : e.getRawText()
                ))
                .collect(Collectors.joining(" "));
    }

    private String callGemini(WebClient client, String prompt, String label) {
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        try {
            return client.post()
                    .uri("/v1beta/models/" + geminiModel + ":generateContent")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GEMINI ({}) ERROR STATUS: {} BODY: {}", label, e.getStatusCode(), e.getResponseBodyAsString());
            return "{\"error\": \"" + e.getStatusCode() + " - " + e.getResponseBodyAsString().replace("\"", "'") + "\"}";
        } catch (Exception e) {
            log.error("GEMINI ({}) call failed: {}", label, e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String callGroq(WebClient client, String prompt, String label) {
        Map<String, Object> body = Map.of(
                "model", groqModel,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            return client.post()
                    .uri("/chat/completions")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GROQ ({}) ERROR STATUS: {} BODY: {}", label, e.getStatusCode(), e.getResponseBodyAsString());
            return "{\"error\": \"" + e.getStatusCode() + " - " + e.getResponseBodyAsString().replace("\"", "'") + "\"}";
        } catch (Exception e) {
            log.error("GROQ ({}) call failed: {}", label, e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Try parsing Gemini's structured JSON out of the response envelope.
     * Returns an "empty" (all-null, ok=false) result if parsing fails, so a
     * bad/unexpected Gemini response never crashes event creation — callers
     * fall back to manual-override fields or defaults.
     */
    private ParsedExtraction parse(String responseBody) {
        ParsedExtraction result = new ParsedExtraction();
        if (responseBody == null || responseBody.isBlank()) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String candidateText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text").asText();
            String cleaned = candidateText.replaceAll("```json|```", "").trim();
            JsonNode parsed = objectMapper.readTree(cleaned);
            result.severity = parsed.path("severity").asInt();
            result.eventType = parsed.path("eventType").asText();
            result.durationDays = parsed.path("durationDays").asInt();
            if (parsed.hasNonNull("routeName")) {
                result.routeName = parsed.path("routeName").asText();
            }
            result.ok = true;
        } catch (Exception e) {
            log.warn("Could not parse structured extraction from a Gemini response, using overrides/defaults: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Same as {@link #parse(String)} but for Groq's OpenAI-style response
     * envelope (choices[0].message.content) instead of Gemini's
     * (candidates[0].content.parts[0].text).
     */
    private ParsedExtraction parseGroq(String responseBody) {
        ParsedExtraction result = new ParsedExtraction();
        if (responseBody == null || responseBody.isBlank()) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String candidateText = root.path("choices").get(0)
                    .path("message").path("content").asText();
            String cleaned = candidateText.replaceAll("```json|```", "").trim();
            JsonNode parsed = objectMapper.readTree(cleaned);
            result.severity = parsed.path("severity").asInt();
            result.eventType = parsed.path("eventType").asText();
            result.durationDays = parsed.path("durationDays").asInt();
            if (parsed.hasNonNull("routeName")) {
                result.routeName = parsed.path("routeName").asText();
            }
            result.ok = true;
        } catch (Exception e) {
            log.warn("Could not parse structured extraction from a Groq response, using overrides/defaults: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Reconciles the primary and secondary key's answers for the same
     * headline. If only one call actually parsed, that one wins outright.
     * If both parsed: severity/duration are averaged (rounded), eventType
     * takes the primary's answer unless only the secondary matched a known
     * route/produced a value, and routeName prefers whichever answer names
     * an actual known route.
     */
    private ParsedExtraction reconcile(ParsedExtraction primary, ParsedExtraction secondary, List<Route> allRoutes) {
        if (primary.ok && !secondary.ok) return primary;
        if (!primary.ok && secondary.ok) return secondary;
        if (!primary.ok && !secondary.ok) return primary; // both empty — nothing to reconcile

        ParsedExtraction merged = new ParsedExtraction();
        merged.ok = true;

        merged.severity = Math.round((primary.severity + secondary.severity) / 2.0f);
        merged.severity = Math.max(0, Math.min(10, merged.severity));

        merged.durationDays = (primary.durationDays != null && secondary.durationDays != null)
                ? Math.round((primary.durationDays + secondary.durationDays) / 2.0f)
                : (primary.durationDays != null ? primary.durationDays : secondary.durationDays);

        if (primary.eventType != null && primary.eventType.equalsIgnoreCase(secondary.eventType)) {
            merged.eventType = primary.eventType;
        } else if (primary.eventType != null && !primary.eventType.isBlank()) {
            merged.eventType = primary.eventType;
            if (secondary.eventType != null && !secondary.eventType.isBlank()) {
                log.info("Gemini keys disagreed on eventType: primary='{}' secondary='{}' — using primary", primary.eventType, secondary.eventType);
            }
        } else {
            merged.eventType = secondary.eventType;
        }

        boolean primaryMatchesRoute = matchesKnownRoute(primary.routeName, allRoutes);
        boolean secondaryMatchesRoute = matchesKnownRoute(secondary.routeName, allRoutes);
        if (primaryMatchesRoute) {
            merged.routeName = primary.routeName;
        } else if (secondaryMatchesRoute) {
            merged.routeName = secondary.routeName;
        } else {
            merged.routeName = primary.routeName;
        }

        return merged;
    }

    private boolean matchesKnownRoute(String routeName, List<Route> allRoutes) {
        if (routeName == null || routeName.isBlank() || "null".equalsIgnoreCase(routeName.trim())) {
            return false;
        }
        String needle = routeName.trim();
        return allRoutes.stream().anyMatch(r ->
                r.getName().equalsIgnoreCase(needle) || r.getName().contains(needle) || needle.contains(r.getName()));
    }

    /**
     * Builds a structured primary-vs-secondary-vs-reconciled comparison for
     * the frontend to render directly (no need to parse Gemini's raw
     * response envelope client-side). Raw envelopes are still included
     * under "raw" for anyone who wants to audit the actual API calls, but
     * that's the part the UI keeps collapsed by default.
     */
    private String combineRawResponses(ParsedExtraction primary, ParsedExtraction secondary, ParsedExtraction merged,
                                       String primaryRaw, String secondaryRaw) {
        Map<String, Object> combined = Map.of(
                "dualKeyComparison", true,
                "primary", extractionAsMap(primary),
                "secondary", extractionAsMap(secondary),
                "reconciled", extractionAsMap(merged),
                "agreement", Map.of(
                        "severity", java.util.Objects.equals(primary.severity, secondary.severity),
                        "eventType", primary.eventType != null && primary.eventType.equalsIgnoreCase(secondary.eventType),
                        "durationDays", java.util.Objects.equals(primary.durationDays, secondary.durationDays),
                        "routeName", primary.routeName != null && primary.routeName.equalsIgnoreCase(secondary.routeName)
                ),
                "raw", Map.of(
                        "primary", primaryRaw == null ? "" : primaryRaw,
                        "secondary", secondaryRaw == null ? "" : secondaryRaw
                )
        );
        try {
            return objectMapper.writeValueAsString(combined);
        } catch (Exception e) {
            // Extremely unlikely (all values are simple strings/maps), but
            // never let serialization of the audit trail break event save.
            return primaryRaw;
        }
    }

    private Map<String, Object> extractionAsMap(ParsedExtraction extraction) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("ok", extraction.ok);
        map.put("severity", extraction.severity != null ? extraction.severity : 0);
        map.put("eventType", extraction.eventType != null ? extraction.eventType : "UNCLASSIFIED");
        map.put("durationDays", extraction.durationDays != null ? extraction.durationDays : 0);
        map.put("routeName", extraction.routeName != null ? extraction.routeName : "");
        return map;
    }

    private static class ParsedExtraction {
        boolean ok = false;
        Integer severity;
        String eventType;
        Integer durationDays;
        String routeName;
    }
}