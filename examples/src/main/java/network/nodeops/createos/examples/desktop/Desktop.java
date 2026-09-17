package network.nodeops.createos.examples.desktop;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.Duration;
import javax.imageio.ImageIO;
import network.nodeops.createos.ComputerService;
import network.nodeops.createos.CreateOsClient;
import network.nodeops.createos.Sandbox;
import network.nodeops.createos.model.ComputerPoint;
import network.nodeops.createos.model.CreateSandboxRequest;
import network.nodeops.createos.model.ScreenGeometry;
import network.nodeops.createos.model.ScreenOptions;
import network.nodeops.createos.model.ScreenshotOptions;

/** Demonstrates screenshots, desktop controls, and a temporary noVNC connection. */
public final class Desktop {
  private static final String PRIMARY_SCREEN = "screen-0";

  private Desktop() {}

  /** Runs the example. */
  public static void main(String[] arguments) throws Exception {
    CreateOsClient client = CreateOsClient.builder().build();
    Sandbox sandbox =
        client.createSandbox(
            CreateSandboxRequest.builder("s-2vcpu-4gb")
                .rootFileSystem("desktop:1")
                .ingressEnabled(true)
                .build());
    try {
      ComputerService computer = sandbox.computer();
      ScreenOptions screen = new ScreenOptions(PRIMARY_SCREEN);
      ScreenGeometry geometry = waitForDesktop(computer, screen);
      var screens = computer.screens().list();
      var primary = computer.screens().get(PRIMARY_SCREEN);
      System.out.printf(
          "screen: %dx%d display=%s%n", geometry.width(), geometry.height(), primary.display());

      try (InputStream png =
          computer.screenshot(
              new ScreenshotOptions(PRIMARY_SCREEN, null, null, null, null, null))) {
        BufferedImage image = ImageIO.read(png);
        System.out.printf("screenshot: %dx%d%n", image.getWidth(), image.getHeight());
      }

      ComputerPoint target =
          new ComputerPoint(
              Math.max(10, geometry.width() / 3), Math.max(10, geometry.height() / 3));
      computer.mouse().move(target, screen);
      System.out.println("cursor: " + computer.cursor(screen));
      String clipboard = "CreateOS desktop " + sandbox.id();
      computer.setClipboard(clipboard, screen);
      System.out.println("clipboard: " + computer.clipboard(screen));
      computer.open("https://example.com", screen);

      var connection = computer.screens().connect(PRIMARY_SCREEN);
      System.out.println("noVNC URL: " + connection.url());
      if (screens.isEmpty()) {
        throw new IllegalStateException("primary screen was not listed");
      }
    } finally {
      sandbox.destroy();
    }
  }

  private static ScreenGeometry waitForDesktop(ComputerService computer, ScreenOptions screen)
      throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
    RuntimeException lastFailure = null;
    while (System.nanoTime() < deadline) {
      try {
        return computer.screen(screen);
      } catch (RuntimeException exception) {
        lastFailure = exception;
        Thread.sleep(Duration.ofSeconds(2).toMillis());
      }
    }
    throw new IllegalStateException("desktop did not become ready", lastFailure);
  }
}
