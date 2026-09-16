package sh.createos.examples.commandstreaming;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import sh.createos.CreateOsClient;
import sh.createos.Sandbox;
import sh.createos.model.CommandStreamEvent;
import sh.createos.model.CreateSandboxRequest;
import sh.createos.model.RunCommandRequest;

/** Uploads a Python program and prints output as the sandbox produces it. */
public final class CommandStreaming {
  private static final String SCRIPT =
      """
      import time

      for number in range(1, 6):
          print(f"result {number}", flush=True)
          time.sleep(1)
      """;

  private CommandStreaming() {}

  /** Runs the example. */
  public static void main(String[] arguments) {
    CreateOsClient client = CreateOsClient.builder().build();
    Sandbox sandbox =
        client.createSandbox(
            CreateSandboxRequest.builder("s-1vcpu-1gb").rootFileSystem("devbox:1").build());
    try {
      sandbox
          .files()
          .upload(
              "/tmp/script.py", new ByteArrayInputStream(SCRIPT.getBytes(StandardCharsets.UTF_8)));
      try (var stream = sandbox.streamCommand(RunCommandRequest.of("python3", "/tmp/script.py"))) {
        for (var event = stream.receive(); event != null; event = stream.receive()) {
          print(event);
        }
      }
    } finally {
      sandbox.destroy();
    }
  }

  private static void print(CommandStreamEvent event) {
    switch (event.type()) {
      case STDOUT -> System.out.print(event.data());
      case STDERR -> System.err.print(event.data());
      case EXIT -> System.out.printf("(exited %d)%n", event.exitCode());
      case ERROR -> System.err.println("agent error: " + event.message());
      case HEARTBEAT -> {
        // Heartbeats carry no output.
      }
      default -> throw new IllegalStateException("unknown command event: " + event.type());
    }
  }
}
