package sh.createos.model;

/** Selects the desktop screen used by a computer operation. */
public record ScreenOptions(String screenId) {
  /** Lets the API select its default screen. */
  public static final ScreenOptions DEFAULT = new ScreenOptions(null);
}
