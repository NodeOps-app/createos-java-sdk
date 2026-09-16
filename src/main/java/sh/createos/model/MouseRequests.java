package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Request models for mouse operations. */
public final class MouseRequests {
  private MouseRequests() {}

  /** Mouse click request. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Click(String button, Integer x, Integer y, Integer count) {}

  /** Vertical mouse-scroll request. */
  public record Scroll(String direction, int amount) {}

  /** Mouse drag request. */
  public record Drag(ComputerPoint from, ComputerPoint to) {}
}
