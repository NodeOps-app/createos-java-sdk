package network.nodeops.createos.examples.network;

import java.time.Instant;
import network.nodeops.createos.CreateOsClient;
import network.nodeops.createos.Sandbox;
import network.nodeops.createos.model.CreateSandboxRequest;
import network.nodeops.createos.model.Network;

/** Attaches a sandbox to a private overlay network and verifies membership. */
public final class NetworkExample {
  private NetworkExample() {}

  /** Runs the example. */
  public static void main(String[] arguments) {
    CreateOsClient client = CreateOsClient.builder().build();
    Network network = client.networks().create("java-sdk-" + Instant.now().getEpochSecond());
    Sandbox sandbox = null;
    try {
      sandbox =
          client.createSandbox(
              CreateSandboxRequest.builder("s-1vcpu-1gb").rootFileSystem("devbox:1").build());
      sandbox.attachNetwork(network.id());
      Network connected = client.networks().get(network.id());
      String sandboxId = sandbox.id();
      Network.Member member =
          connected.members().stream()
              .filter(candidate -> candidate.sandboxId().equals(sandboxId))
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("sandbox is not a network member"));
      System.out.printf(
          "verified member: sandbox=%s ip=%s status=%s%n",
          member.sandboxId(), member.ipAddress(), member.status());
    } finally {
      if (sandbox != null) {
        sandbox.destroy();
      }
      client.networks().delete(network.id());
    }
  }
}
