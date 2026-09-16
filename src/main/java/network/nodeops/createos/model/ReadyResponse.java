package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Control-plane readiness state. */
public record ReadyResponse(
    boolean ready,
    String reason,
    @JsonProperty("scheduler_last_ok_ms_ago") long schedulerLastOkMillisecondsAgo) {}
