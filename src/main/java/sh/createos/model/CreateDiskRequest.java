package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import sh.createos.model.Disk.DiskConfig;

/** Registers an S3-compatible persistent disk. */
public record CreateDiskRequest(
    String name, String kind, DiskConfig config, DiskCredentials credentials) {
  /** Write-only S3-compatible credentials. */
  public record DiskCredentials(
      @JsonProperty("access_key") String accessKey, @JsonProperty("secret_key") String secretKey) {}
}
