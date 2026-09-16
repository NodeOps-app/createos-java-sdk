package network.nodeops.createos.examples.filesandsnapshots;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import network.nodeops.createos.CreateOsClient;
import network.nodeops.createos.Sandbox;
import network.nodeops.createos.model.CreateSandboxRequest;
import network.nodeops.createos.model.ForkSandboxRequest;
import network.nodeops.createos.model.RunCommandRequest;

/** Demonstrates copy-on-write sandbox snapshots and diverging filesystems. */
public final class FilesAndSnapshots {
  private static final String BASE_PATH = "/root/seed.txt";
  private static final String FORK_ONLY_PATH = "/root/fork-only.txt";

  private FilesAndSnapshots() {}

  /** Runs the example. */
  public static void main(String[] arguments) {
    CreateOsClient client = CreateOsClient.builder().build();
    Sandbox base =
        client.createSandbox(
            CreateSandboxRequest.builder("s-1vcpu-256mb").rootFileSystem("devbox:1").build());
    Sandbox fork = null;
    try {
      upload(base, BASE_PATH, "seed written at " + Instant.now() + "\n");
      System.out.println("base: " + read(base, BASE_PATH));
      base.pause();
      base.waitUntilPaused(Duration.ofMinutes(10));

      fork = base.fork(new ForkSandboxRequest(true, null, null, null, null));
      fork.waitUntilPaused(Duration.ofMinutes(10));
      fork.resume();
      fork.waitUntilRunning(Duration.ofMinutes(5));
      System.out.println("fork inherited: " + read(fork, BASE_PATH));
      upload(fork, FORK_ONLY_PATH, "written only in fork at " + Instant.now() + "\n");

      base.resume();
      base.waitUntilRunning(Duration.ofMinutes(5));
      System.out.println("base fork-only file: " + read(base, FORK_ONLY_PATH));
      System.out.println("base seed: " + read(base, BASE_PATH));
    } finally {
      if (fork != null) {
        fork.destroy();
      }
      base.destroy();
    }
  }

  private static void upload(Sandbox sandbox, String path, String contents) {
    sandbox
        .files()
        .upload(path, new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));
  }

  private static String read(Sandbox sandbox, String path) {
    return sandbox
        .runCommand(RunCommandRequest.of("sh", "-c", "cat " + path + " 2>&1"))
        .result()
        .standardOutput()
        .trim();
  }
}
