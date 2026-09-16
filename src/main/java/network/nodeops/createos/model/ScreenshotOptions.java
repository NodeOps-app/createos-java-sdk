package network.nodeops.createos.model;

/** Selects a screen, window, or crop for PNG screenshot capture. */
public record ScreenshotOptions(
    String screenId, String windowId, Integer x, Integer y, Integer width, Integer height) {
  /** Captures the API's default screen. */
  public static final ScreenshotOptions DEFAULT =
      new ScreenshotOptions(null, null, null, null, null, null);
}
