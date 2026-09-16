package network.nodeops.createos.examples.managedprocess;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import network.nodeops.createos.CreateOsClient;
import network.nodeops.createos.ProcessStream;
import network.nodeops.createos.Sandbox;
import network.nodeops.createos.model.CreateProcessRequest;
import network.nodeops.createos.model.CreateSandboxRequest;
import network.nodeops.createos.model.ProcessStreamEvent;
import network.nodeops.createos.model.PtySize;

/** Demonstrates persistent pipe and PTY process lifecycles. */
public final class ManagedProcessExample {
  private ManagedProcessExample() {}

  /** Runs the example. */
  public static void main(String[] arguments) {
    CreateOsClient client = CreateOsClient.builder().build();
    Sandbox sandbox =
        client.createSandbox(
            CreateSandboxRequest.builder("s-1vcpu-1gb")
                .rootFileSystem("devbox:1")
                .environmentVariables(Map.of("PROCESS_DEMO_BASE", "from-sandbox-env"))
                .build());
    try {
      var processes = sandbox.processes();
      var pipe =
          processes.create(
              new CreateProcessRequest(
                  "/bin/sh",
                  List.of(
                      "-c",
                      "printf 'base:%s\\n' \"$PROCESS_DEMO_BASE\"; "
                          + "printf 'stderr:ready\\n' >&2; "
                          + "IFS= read -r line; printf 'stdin:%s\\n' \"$line\""),
                  "/root",
                  Map.of(),
                  null));
      processes.input(pipe.processId(), "hello managed process\n");
      processes.closeStandardInput(pipe.processId());
      processes.waitFor(pipe.processId(), "tree", Duration.ofSeconds(5));
      print(processes.connect(pipe.processId(), 0));

      var terminal =
          processes.create(
              new CreateProcessRequest(null, List.of(), "/root", Map.of(), new PtySize(24, 80)));
      processes.input(terminal.processId(), "echo terminal-ready; stty size\n");
      processes.resize(terminal.processId(), new PtySize(32, 100));
      processes.input(terminal.processId(), "echo after-resize; stty size; exit\n");
      processes.waitFor(terminal.processId(), "tree", Duration.ofSeconds(5));
      print(processes.connect(terminal.processId(), 0));

      var longRunning =
          processes.create(
              new CreateProcessRequest(
                  "/bin/sh",
                  List.of("-c", "trap '' TERM; sleep 300 & wait"),
                  null,
                  Map.of(),
                  null));
      var terminated = processes.delete(longRunning.processId(), Duration.ofMillis(100));
      System.out.println("terminated tree: " + terminated.treeExited());
    } finally {
      sandbox.destroy();
    }
  }

  private static void print(ProcessStream stream) {
    try (stream) {
      for (ProcessStreamEvent event = stream.receive(); event != null; event = stream.receive()) {
        if ("data".equals(event.type())) {
          String value = new String(event.data(), StandardCharsets.UTF_8);
          if ("stderr".equals(event.stream())) {
            System.err.print(value);
          } else {
            System.out.print(value);
          }
        } else if ("error".equals(event.type())) {
          throw new IllegalStateException(event.errorMessage());
        }
      }
    }
  }
}
