package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Plaintext delegated token returned only when created or rotated. */
public record SandboxAccessTokenCreateResponse(
    String token,
    boolean enabled,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("rotated_at") Instant rotatedAt) {
  @Override
  public String toString() {
    return "SandboxAccessTokenCreateResponse[token=[REDACTED], enabled=" + enabled + "]";
  }
}
