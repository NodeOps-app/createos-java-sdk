package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Sandbox bandwidth quota and usage. */
public record Bandwidth(
    String id,
    @JsonProperty("quota_bytes") long quotaBytes,
    @JsonProperty("used_bytes") long usedBytes,
    @JsonProperty("ingress_bytes") long ingressBytes,
    @JsonProperty("remaining_bytes") long remainingBytes,
    boolean capped) {}
