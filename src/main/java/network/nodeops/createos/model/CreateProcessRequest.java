package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** Persistent managed process or PTY creation request. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CreateProcessRequest(
    @JsonProperty("cmd") String command,
    @JsonProperty("args") List<String> arguments,
    @JsonProperty("cwd") String workingDirectory,
    @JsonProperty("env") Map<String, String> environmentVariables,
    @JsonProperty("pty") PtySize pty) {
  /** Defensively copies collection values. */
  public CreateProcessRequest {
    arguments = arguments == null ? List.of() : List.copyOf(arguments);
    environmentVariables =
        environmentVariables == null ? Map.of() : Map.copyOf(environmentVariables);
  }
}
