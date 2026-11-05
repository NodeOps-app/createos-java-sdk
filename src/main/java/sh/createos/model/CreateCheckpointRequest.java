package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/** Selects a directory whose local file history should be captured. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CreateCheckpointRequest(String path, String name) {
  /** Validates the required directory path. */
  public CreateCheckpointRequest {
    Objects.requireNonNull(path, "path");
  }
}
