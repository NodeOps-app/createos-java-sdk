package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

/** Lifecycle state of a sandbox. */
public enum SandboxStatus {
  CREATING("creating"),
  RUNNING("running"),
  PAUSING("pausing"),
  PAUSED("paused"),
  RESUMING("resuming"),
  FORKING("forking"),
  ERROR("error"),
  DESTROYING("destroying"),
  DESTROYED("destroyed"),
  FAILED("failed"),
  @JsonEnumDefaultValue
  UNKNOWN("unknown");

  private final String wireValue;

  SandboxStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  /** Returns the server wire value. */
  @JsonValue
  public String wireValue() {
    return wireValue;
  }
}
