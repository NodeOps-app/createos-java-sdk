package sh.createos.examples.snapshots;

import java.time.Duration;
import java.time.Instant;
import sh.createos.CreateOsClient;
import sh.createos.Sandbox;
import sh.createos.model.CreateSandboxRequest;
import sh.createos.model.CreateSnapshotRequest;
import sh.createos.model.Snapshot;
import sh.createos.model.SnapshotStatus;
import sh.createos.model.SnapshotType;

/** Captures a reusable disk snapshot and restores a second sandbox from it. */
public final class Snapshots {
  private Snapshots() {}

  /** Runs the example. */
  public static void main(String[] arguments) throws InterruptedException {
    CreateOsClient client =
        CreateOsClient.builder().apiKey(System.getenv("CREATEOS_API_KEY")).build();
    Sandbox source =
        client.createSandbox(
            CreateSandboxRequest.builder("s-1vcpu-1gb").rootFileSystem("alpine:3.20").build());
    Snapshot snapshot = null;
    Sandbox restored = null;
    try {
      snapshot =
          source.createSnapshot(
              new CreateSnapshotRequest(SnapshotType.DISK, "configured-workspace"));
      snapshot = waitUntilReady(client, snapshot.id(), Duration.ofMinutes(5));
      restored = client.createSandbox(CreateSandboxRequest.fromSnapshot(snapshot.id()).build());
      System.out.printf("restored sandbox %s from snapshot %s%n", restored.id(), snapshot.id());
    } finally {
      if (restored != null) {
        restored.destroy();
      }
      source.destroy();
      if (snapshot != null && snapshot.status() != SnapshotStatus.CREATING) {
        client.snapshots().delete(snapshot.id());
      }
    }
  }

  private static Snapshot waitUntilReady(CreateOsClient client, String id, Duration timeout)
      throws InterruptedException {
    Instant deadline = Instant.now().plus(timeout);
    while (Instant.now().isBefore(deadline)) {
      Snapshot snapshot = client.snapshots().get(id);
      if (snapshot.status() == SnapshotStatus.READY) {
        return snapshot;
      }
      if (snapshot.status() == SnapshotStatus.FAILED) {
        throw new IllegalStateException("snapshot capture failed: " + id);
      }
      Thread.sleep(1000);
    }
    throw new IllegalStateException("timed out waiting for snapshot: " + id);
  }
}
