package com.winspect.apispec.controller.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "apispec")
@Data
public class ControllerConfig {

    /** Organization ID (from operator config) */
    private String orgId;

    /** API key for authenticating with central API */
    private String apiKey;

    /** Central API base URL */
    private String centralApiUrl;

    /** Cluster identifier (optional) */
    private String clusterId;

    /** Namespaces to watch (empty = all namespaces) */
    private java.util.List<String> namespaces = java.util.List.of();
}
