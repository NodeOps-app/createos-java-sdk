package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Raw newline-delimited command stream frame. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CommandStreamFrame(
    @JsonProperty("stdout") String standardOutput,
    @JsonProperty("stderr") String standardError,
    @JsonProperty("exit_code") Integer exitCode,
    @JsonProperty("error") String errorMessage,
    @JsonProperty("hb") boolean heartbeat) {}
