package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

/** Lifecycle state of a reusable snapshot. */
public enum SnapshotStatus {
  CREATING("creating"),
  READY("ready"),
  FAILED("failed"),
  DELETING("deleting"),
  @JsonEnumDefaultValue
  UNKNOWN("unknown");

  private final String wireValue;

  SnapshotStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  /** Returns the server wire value. */
  @JsonValue
  public String wireValue() {
    return wireValue;
  }
}
