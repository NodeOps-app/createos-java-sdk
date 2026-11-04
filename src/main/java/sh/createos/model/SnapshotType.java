package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonValue;

/** State included in a reusable snapshot. */
public enum SnapshotType {
  DISK("disk"),
  FULL("full");

  private final String wireValue;

  SnapshotType(String wireValue) {
    this.wireValue = wireValue;
  }

  /** Returns the server wire value. */
  @JsonValue
  public String wireValue() {
    return wireValue;
  }
}
