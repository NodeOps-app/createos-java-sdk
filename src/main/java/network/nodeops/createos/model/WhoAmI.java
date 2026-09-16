package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Identity associated with the configured API key. */
public record WhoAmI(@JsonProperty("user_id") String userId, Stats stats) {
  /** Sandbox counts visible to the caller. */
  public record Stats(int running, int paused, int other, int total) {}
}
