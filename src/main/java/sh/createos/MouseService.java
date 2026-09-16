package sh.createos;

import java.util.Map;
import sh.createos.model.ComputerPoint;
import sh.createos.model.MouseRequests.Click;
import sh.createos.model.MouseRequests.Drag;
import sh.createos.model.MouseRequests.Scroll;
import sh.createos.model.ScreenOptions;

/** Mouse operations for a sandbox desktop. */
public final class MouseService {
  private final ComputerService computer;

  MouseService(ComputerService computer) {
    this.computer = computer;
  }

  /** Moves the cursor. */
  public void move(ComputerPoint point, ScreenOptions options) {
    action("/mouse/move", options, point);
  }

  /** Clicks a mouse button. */
  public void click(Click request, ScreenOptions options) {
    action("/mouse/click", options, request);
  }

  /** Scrolls vertically. */
  public void scroll(Scroll request, ScreenOptions options) {
    action("/mouse/scroll", options, request);
  }

  /** Drags the cursor between two points. */
  public void drag(Drag request, ScreenOptions options) {
    action("/mouse/drag", options, request);
  }

  /** Presses a mouse button without releasing it. */
  public void down(String button, ScreenOptions options) {
    action("/mouse/down", options, Map.of("button", button));
  }

  /** Releases a mouse button. */
  public void up(String button, ScreenOptions options) {
    action("/mouse/up", options, Map.of("button", button));
  }

  private void action(String path, ScreenOptions options, Object body) {
    computer.request("POST", path, options, body, Void.class);
  }
}
