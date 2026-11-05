package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Local file history retained inside one sandbox. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Checkpoint(
    String id,
    String path,
    String name,
    @JsonProperty("parent_id") String parentId,
    @JsonProperty("created_at") Instant createdAt) {}
