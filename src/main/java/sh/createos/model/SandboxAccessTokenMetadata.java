package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Delegated token state without plaintext credential material. */
public record SandboxAccessTokenMetadata(
    boolean enabled,
    @JsonProperty("token_hint") String tokenHint,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("rotated_at") Instant rotatedAt) {}
