package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Buffered command result and execution timing. */
public record RunCommandResponse(
    CommandResult result, @JsonProperty("exec_ms") double executionMilliseconds) {}
