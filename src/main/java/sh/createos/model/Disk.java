package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Registered persistent disk without credentials. */
public record Disk(
    String id,
    String name,
    String kind,
    DiskConfig config,
    @JsonProperty("created_at") Instant createdAt) {
  /** Non-secret S3-compatible disk configuration. */
  public record DiskConfig(
      String bucket,
      String endpoint,
      String region,
      @JsonProperty("use_path_style") boolean usePathStyle) {}
}
