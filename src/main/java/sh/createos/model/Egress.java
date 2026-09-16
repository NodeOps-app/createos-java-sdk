package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Current sandbox egress allowlist. */
public record Egress(String id, @JsonProperty("egress") List<String> rules) {}
