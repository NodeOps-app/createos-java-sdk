package sh.createos;

import java.util.List;
import java.util.Map;
import sh.createos.model.ScreenOptions;

/** Keyboard operations for a sandbox desktop. */
public final class KeyboardService {
  private final ComputerService computer;

  KeyboardService(ComputerService computer) {
    this.computer = computer;
  }

  /** Types text into the active application. */
  public void type(String text, int delayMilliseconds, ScreenOptions options) {
    computer.request(
        "POST",
        "/keyboard/type",
        options,
        Map.of("text", text, "delay_in_ms", delayMilliseconds),
        Void.class);
  }

  /** Presses and releases a key combination. */
  public void press(List<String> keys, ScreenOptions options) {
    keys("/keyboard/press", keys, options);
  }

  /** Presses keys without releasing them. */
  public void down(List<String> keys, ScreenOptions options) {
    keys("/keyboard/down", keys, options);
  }

  /** Releases keys. */
  public void up(List<String> keys, ScreenOptions options) {
    keys("/keyboard/up", keys, options);
  }

  private void keys(String path, List<String> keys, ScreenOptions options) {
    computer.request("POST", path, options, Map.of("keys", keys), Void.class);
  }
}
