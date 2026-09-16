package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Command to execute inside a sandbox. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RunCommandRequest(
    @JsonProperty("cmd") String command,
    @JsonProperty("args") List<String> arguments,
    @JsonProperty("stdin") String standardInput,
    @JsonProperty("env") Map<String, String> environmentVariables,
    boolean stream) {
  /** Validates the command and defensively copies collection values. */
  public RunCommandRequest {
    command = Objects.requireNonNull(command, "command");
    arguments = arguments == null ? List.of() : List.copyOf(arguments);
    environmentVariables =
        environmentVariables == null ? Map.of() : Map.copyOf(environmentVariables);
  }

  /** Creates a buffered command with no input or environment overrides. */
  public static RunCommandRequest of(String command, String... arguments) {
    return new RunCommandRequest(command, List.of(arguments), null, Map.of(), false);
  }

  /** Returns a copy configured for streaming execution. */
  public RunCommandRequest streaming() {
    return new RunCommandRequest(command, arguments, standardInput, environmentVariables, true);
  }
}
