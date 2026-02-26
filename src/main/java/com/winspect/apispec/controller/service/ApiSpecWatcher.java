package com.winspect.apispec.controller.service;

import com.winspect.apispec.controller.config.ControllerConfig;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.Config;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Watches ApiSpec CRDs and syncs to central platform. On Add/Update, syncs.
 * On Delete, does nothing (per plan). Uses periodic list to discover ApiSpecs.
 */
@Slf4j
@Service
public class ApiSpecWatcher {

    private static final String GROUP = "winspect.io";
    private static final String VERSION = "v1";
    private static final String PLURAL = "apispecs";

    private final ControllerConfig config;
    private final CentralApiClient centralApiClient;
    private final ApiClient k8sClient;
    private final CustomObjectsApi customObjectsApi;
    private volatile boolean enabled = false;

    public ApiSpecWatcher(ControllerConfig config, CentralApiClient centralApiClient) {
        this.config = config;
        this.centralApiClient = centralApiClient;
        try {
            this.k8sClient = initK8sClient();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Kubernetes client", e);
        }
        this.customObjectsApi = new CustomObjectsApi(k8sClient);
    }

    private ApiClient initK8sClient() throws IOException {
        try {
            log.info("Using in-cluster Kubernetes configuration");
            return Config.fromCluster();
        } catch (Exception e) {
            log.info("Using kubeconfig for Kubernetes configuration");
            return Config.defaultClient();
        }
    }

    @PostConstruct
    public void init() {
        if (config.getOrgId() != null && !config.getOrgId().isBlank()
                && config.getApiKey() != null && !config.getApiKey().isBlank()
                && config.getCentralApiUrl() != null && !config.getCentralApiUrl().isBlank()) {
            enabled = true;
            log.info("ApiSpec watcher enabled");
        } else {
            log.warn("ApiSpec watcher disabled: missing orgId, apiKey, or centralApiUrl");
        }
    }

    @PreDestroy
    public void stop() {
        enabled = false;
        log.info("ApiSpec watcher stopped");
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${apispec.sync-interval-ms:60000}")
    @SuppressWarnings("unchecked")
    public void syncAll() {
        if (!enabled) return;
        try {
            Object result = customObjectsApi.listClusterCustomObject(GROUP, VERSION, PLURAL)
                    .execute();

            Map<String, Object> list = (Map<String, Object>) result;
            List<Map<String, Object>> items = (List<Map<String, Object>>) list.get("items");
            if (items == null) return;

            for (Map<String, Object> resource : items) {
                Map<String, Object> metadata = (Map<String, Object>) resource.get("metadata");
                Map<String, Object> spec = (Map<String, Object>) resource.get("spec");
                if (metadata == null || spec == null) continue;
                handleAddOrUpdate(resource, metadata, spec);
            }
        } catch (ApiException e) {
            log.warn("ApiSpec list error: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleAddOrUpdate(Map<String, Object> resource, Map<String, Object> metadata, Map<String, Object> spec) {
        String name = (String) spec.get("name");
        String namespace = (String) metadata.get("namespace");
        if (namespace == null) namespace = "default";

        // Filter by namespaces if configured
        if (!config.getNamespaces().isEmpty() && !config.getNamespaces().contains(namespace)) {
            return;
        }

        String openapiSpec = extractOpenApiSpec(resource, namespace);
        if (openapiSpec == null || openapiSpec.isBlank()) {
            log.warn("ApiSpec {}/{} has no OpenAPI content, skipping", namespace, name);
            return;
        }

        UUID orgId = UUID.fromString(config.getOrgId());
        CentralApiClient.SyncApiSpecRequest request = new CentralApiClient.SyncApiSpecRequest(
                orgId,
                name,
                namespace,
                (String) spec.get("owner"),
                (String) spec.get("type"),
                (String) spec.get("gitSha"),
                (String) spec.get("githubRepo"),
                (String) spec.get("githubBranch"),
                (String) spec.get("specFilePath"),
                openapiSpec
        );

        centralApiClient.syncApiSpec(request).block();
    }

    @SuppressWarnings("unchecked")
    private String extractOpenApiSpec(Map<String, Object> resource, String namespace) {
        Map<String, Object> spec = (Map<String, Object>) resource.get("spec");
        if (spec == null) return null;

        Map<String, Object> definition = (Map<String, Object>) spec.get("definition");
        if (definition == null) return null;

        Object inline = definition.get("inline");
        if (inline != null && inline instanceof String) {
            return (String) inline;
        }

        Map<String, Object> configMapRef = (Map<String, Object>) definition.get("configMapRef");
        if (configMapRef != null) {
            String cmName = (String) configMapRef.get("name");
            String key = (String) configMapRef.get("key");
            if (cmName != null && key != null) {
                return readConfigMap(namespace, cmName, key);
            }
        }
        return null;
    }

    private String readConfigMap(String namespace, String name, String key) {
        try {
            io.kubernetes.client.openapi.apis.CoreV1Api coreApi = new io.kubernetes.client.openapi.apis.CoreV1Api(k8sClient);
            io.kubernetes.client.openapi.models.V1ConfigMap cm = coreApi.readNamespacedConfigMap(name, namespace)
                    .execute();
            if (cm != null && cm.getData() != null && cm.getData().containsKey(key)) {
                return cm.getData().get(key);
            }
        } catch (ApiException e) {
            log.error("Failed to read ConfigMap {}/{}: {}", namespace, name, e.getMessage());
        }
        return null;
    }
}
