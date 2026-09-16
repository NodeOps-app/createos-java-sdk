package network.nodeops.createos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import network.nodeops.createos.internal.HttpTransport;
import network.nodeops.createos.model.ComputerWindow;
import network.nodeops.createos.model.ScreenOptions;
import network.nodeops.createos.model.WindowGeometry;

/** Desktop window operations for one sandbox. */
public final class WindowsService {
  private final ComputerService computer;

  WindowsService(ComputerService computer) {
    this.computer = computer;
  }

  /** Lists visible windows, optionally filtered by application. */
  public List<ComputerWindow> list(String application, ScreenOptions options) {
    Map<String, String> query = new HashMap<>(ComputerService.screenQuery(options.screenId()));
    if (application != null && !application.isBlank()) {
      query.put("application", application);
    }
    return computer
        .sandbox()
        .transport()
        .sendList(
            "GET",
            computer.path("/windows"),
            query,
            RequestOptions.DEFAULT,
            false,
            ComputerWindow.class);
  }

  /** Returns the active window. */
  public ComputerWindow current(ScreenOptions options) {
    return computer.get("/windows/current", options, ComputerWindow.class);
  }

  /** Returns a window by identifier. */
  public ComputerWindow get(String windowId, ScreenOptions options) {
    return computer.get(
        "/windows/" + HttpTransport.encodePathSegment(windowId), options, ComputerWindow.class);
  }

  /** Returns a window's position and dimensions. */
  public WindowGeometry geometry(String windowId, ScreenOptions options) {
    return computer.get(
        "/windows/" + HttpTransport.encodePathSegment(windowId) + "/geometry",
        options,
        WindowGeometry.class);
  }

  /** Focuses a window. */
  public void focus(String windowId, ScreenOptions options) {
    action(windowId, "focus", options, null);
  }

  /** Moves a window. */
  public void move(String windowId, int x, int y, ScreenOptions options) {
    action(windowId, "move", options, Map.of("x", x, "y", y));
  }

  /** Resizes a window. */
  public void resize(String windowId, int width, int height, ScreenOptions options) {
    action(windowId, "resize", options, Map.of("width", width, "height", height));
  }

  /** Maximizes a window. */
  public void maximize(String windowId, ScreenOptions options) {
    action(windowId, "maximize", options, null);
  }

  /** Minimizes a window. */
  public void minimize(String windowId, ScreenOptions options) {
    action(windowId, "minimize", options, null);
  }

  /** Restores a minimized or maximized window. */
  public void restore(String windowId, ScreenOptions options) {
    action(windowId, "restore", options, null);
  }

  /** Closes a window. */
  public void close(String windowId, ScreenOptions options) {
    computer.request(
        "DELETE",
        "/windows/" + HttpTransport.encodePathSegment(windowId),
        options,
        null,
        Void.class);
  }

  private void action(String windowId, String action, ScreenOptions options, Object body) {
    computer.request(
        "POST",
        "/windows/" + HttpTransport.encodePathSegment(windowId) + "/" + action,
        options,
        body,
        Void.class);
  }
}
