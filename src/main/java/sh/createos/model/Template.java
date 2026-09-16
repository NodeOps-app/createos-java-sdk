package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Custom root filesystem template. */
public record Template(
    String id,
    String name,
    String base,
    String status,
    @JsonProperty("ext4_size_bytes") long ext4SizeBytes,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("built_at") Instant builtAt,
    String dockerfile) {}
