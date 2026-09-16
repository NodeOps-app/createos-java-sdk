package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** One template build-log event. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplateLogEvent(
    @JsonProperty("ts") Instant timestamp,
    String level,
    String line,
    int attempt,
    @JsonProperty("final") boolean isFinal,
    String status) {}
