package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Configured desktop screen. */
public record ComputerScreen(
    @JsonProperty("screen_id") String screenId,
    String display,
    int width,
    int height,
    @JsonProperty("vnc_port") int vncPort,
    @JsonProperty("novnc_port") int noVncPort) {}
