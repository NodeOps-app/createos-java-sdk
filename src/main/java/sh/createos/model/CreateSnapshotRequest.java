package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/** Request used to capture a reusable snapshot. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CreateSnapshotRequest(SnapshotType type, String name) {
  /** Validates the required snapshot type. */
  public CreateSnapshotRequest {
    Objects.requireNonNull(type, "type");
  }
}
