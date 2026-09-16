package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Temporary noVNC connection details for a desktop screen. */
public record ScreenConnection(
    @JsonProperty("screen_id") String screenId,
    int port,
    String path,
    String token,
    @JsonProperty("expires_at") Instant expiresAt,
    String url) {}
