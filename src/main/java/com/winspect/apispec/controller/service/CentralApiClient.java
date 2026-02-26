package com.winspect.apispec.controller.service;

import com.winspect.apispec.controller.config.ControllerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Client for syncing ApiSpec to central platform backend.
 */
@Slf4j
@Service
public class CentralApiClient {

    private final WebClient webClient;
    private final ControllerConfig config;

    public CentralApiClient(ControllerConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.webClient = webClientBuilder
                .baseUrl(config.getCentralApiUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .build();
    }

    /**
     * Sync API spec from ApiSpec CRD to central platform.
     */
    public Mono<SyncApiSpecResponse> syncApiSpec(SyncApiSpecRequest request) {
        log.info("Syncing ApiSpec: name={}, namespace={}", request.name(), request.namespace());

        return webClient.post()
                .uri("/api-management/discovery/spec-records/sync")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SyncApiSpecResponse.class)
                .doOnSuccess(r -> log.info("Synced ApiSpec: apiId={}, created={}", r.apiId(), r.created()))
                .doOnError(e -> log.error("Failed to sync ApiSpec: {}", request.name(), e))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(t -> !(t instanceof org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest)));
    }

    public record SyncApiSpecRequest(
            UUID orgId,
            String name,
            String namespace,
            String owner,
            String type,
            String gitSha,
            String githubRepo,
            String githubBranch,
            String specFilePath,
            String openapiSpec
    ) {}

    public record SyncApiSpecResponse(
            UUID apiId,
            UUID specRecordId,
            Boolean created
    ) {}
}
