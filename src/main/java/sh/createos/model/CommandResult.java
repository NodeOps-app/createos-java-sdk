package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Buffered command output and exit state. */
public record CommandResult(
    @JsonProperty("stdout") String standardOutput,
    @JsonProperty("stderr") String standardError,
    @JsonProperty("exit_code") int exitCode,
    @JsonProperty("error") String errorMessage) {}
