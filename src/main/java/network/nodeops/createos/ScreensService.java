package network.nodeops.createos;

import java.util.List;
import java.util.Map;
import network.nodeops.createos.internal.HttpTransport;
import network.nodeops.createos.model.ComputerScreen;
import network.nodeops.createos.model.ScreenConnection;

/** Desktop screen and noVNC connection operations for one sandbox. */
public final class ScreensService {
  private final ComputerService computer;

  ScreensService(ComputerService computer) {
    this.computer = computer;
  }

  /** Lists configured desktop screens. */
  public List<ComputerScreen> list() {
    return computer
        .sandbox()
        .transport()
        .sendList(
            "GET",
            computer.path("/screens"),
            Map.of(),
            RequestOptions.DEFAULT,
            false,
            ComputerScreen.class);
  }

  /** Creates a desktop screen. */
  public ComputerScreen create(int width, int height) {
    return computer
        .sandbox()
        .transport()
        .send(
            "POST",
            computer.path("/screens"),
            Map.of(),
            Map.of("width", width, "height", height),
            RequestOptions.DEFAULT,
            false,
            ComputerScreen.class);
  }

  /** Returns a desktop screen. */
  public ComputerScreen get(String screenId) {
    return request("GET", screenId, "", null, ComputerScreen.class);
  }

  /** Returns a temporary noVNC connection for a screen. */
  public ScreenConnection connect(String screenId) {
    return request("GET", screenId, "/connect", null, ScreenConnection.class);
  }

  /** Resizes a desktop screen. */
  public ComputerScreen resize(String screenId, int width, int height) {
    return request(
        "POST",
        screenId,
        "/resize",
        Map.of("width", width, "height", height),
        ComputerScreen.class);
  }

  /** Deletes a desktop screen. */
  public void delete(String screenId) {
    request("DELETE", screenId, "", null, Void.class);
  }

  private <T> T request(String method, String screenId, String suffix, Object body, Class<T> type) {
    return computer
        .sandbox()
        .transport()
        .send(
            method,
            computer.path("/screens/" + HttpTransport.encodePathSegment(screenId) + suffix),
            Map.of(),
            body,
            RequestOptions.DEFAULT,
            false,
            type);
  }
}
