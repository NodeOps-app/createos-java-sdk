package sh.createos;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;
import sh.createos.internal.RawResponse;
import sh.createos.model.ComputerPoint;
import sh.createos.model.ScreenGeometry;
import sh.createos.model.ScreenOptions;
import sh.createos.model.ScreenshotOptions;

/** Desktop computer-use operations for one sandbox. */
public final class ComputerService {
  private final Sandbox sandbox;
  private final MouseService mouse;
  private final KeyboardService keyboard;
  private final WindowsService windows;
  private final ScreensService screens;

  ComputerService(Sandbox sandbox) {
    this.sandbox = sandbox;
    this.mouse = new MouseService(this);
    this.keyboard = new KeyboardService(this);
    this.windows = new WindowsService(this);
    this.screens = new ScreensService(this);
  }

  /** Returns mouse operations. */
  public MouseService mouse() {
    return mouse;
  }

  /** Returns keyboard operations. */
  public KeyboardService keyboard() {
    return keyboard;
  }

  /** Returns desktop-window operations. */
  public WindowsService windows() {
    return windows;
  }

  /** Returns desktop-screen operations. */
  public ScreensService screens() {
    return screens;
  }

  /** Captures PNG bytes. The caller must close the returned stream. */
  public InputStream screenshot(ScreenshotOptions options) {
    Map<String, String> query = screenQuery(options.screenId());
    put(query, "window_id", options.windowId());
    put(query, "x", options.x());
    put(query, "y", options.y());
    put(query, "width", options.width());
    put(query, "height", options.height());
    String path = path("/screenshot");
    RawResponse response =
        sandbox
            .transport()
            .sendRaw(
                "GET",
                path,
                query,
                HttpRequest.BodyPublishers.noBody(),
                RequestOptions.DEFAULT,
                false,
                false);
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      try (var body = response.body()) {
        sandbox
            .transport()
            .requireSuccess("GET", path, response, sandbox.transport().readErrorBody(body));
      } catch (IOException exception) {
        throw new IllegalStateException("Failed to read screenshot error", exception);
      }
    }
    return response.body();
  }

  /** Returns the selected screen's dimensions. */
  public ScreenGeometry screen(ScreenOptions options) {
    return get("/screen", options, ScreenGeometry.class);
  }

  /** Returns the current cursor position. */
  public ComputerPoint cursor(ScreenOptions options) {
    return get("/cursor", options, ComputerPoint.class);
  }

  /** Returns the current clipboard text. */
  public String clipboard(ScreenOptions options) {
    return get("/clipboard", options, Clipboard.class).text();
  }

  /** Replaces clipboard text. */
  public void setClipboard(String text, ScreenOptions options) {
    request("PUT", "/clipboard", options, Map.of("text", text), Void.class);
  }

  /** Opens a URL or desktop target. */
  public void open(String target, ScreenOptions options) {
    request("POST", "/open", options, Map.of("target", target), Void.class);
  }

  /** Launches an installed desktop application. */
  public void launch(String application, String uri, ScreenOptions options) {
    Map<String, String> body = new HashMap<>();
    body.put("application", application);
    if (uri != null && !uri.isBlank()) {
      body.put("uri", uri);
    }
    request("POST", "/launch", options, body, Void.class);
  }

  <T> T get(String suffix, ScreenOptions options, Class<T> type) {
    return request("GET", suffix, options, null, type);
  }

  <T> T request(String method, String suffix, ScreenOptions options, Object body, Class<T> type) {
    return sandbox
        .transport()
        .send(
            method,
            path(suffix),
            screenQuery(options.screenId()),
            body,
            RequestOptions.DEFAULT,
            false,
            type);
  }

  Sandbox sandbox() {
    return sandbox;
  }

  String path(String suffix) {
    return sandbox.path("/computer" + suffix);
  }

  static Map<String, String> screenQuery(String screenId) {
    Map<String, String> query = new HashMap<>();
    put(query, "screen_id", screenId);
    return query;
  }

  private static void put(Map<String, String> values, String key, Object value) {
    if (value != null && !value.toString().isBlank()) {
      values.put(key, value.toString());
    }
  }

  private record Clipboard(String text) {}
}
