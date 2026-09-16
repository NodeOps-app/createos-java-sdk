package sh.createos.model;

/** Control-plane liveness state. */
public record HealthResponse(boolean up) {}
