package sh.createos.examples.checkpoints;

import sh.createos.CreateOsClient;
import sh.createos.Sandbox;
import sh.createos.model.Checkpoint;
import sh.createos.model.CreateCheckpointRequest;
import sh.createos.model.CreateSandboxRequest;

/** Captures and restores local directory history inside one sandbox. */
public final class Checkpoints {
  private static final String PATH = "/workspace/checkpoint-demo";

  private Checkpoints() {}

  /** Runs the example. */
  public static void main(String[] arguments) {
    CreateOsClient client =
        CreateOsClient.builder().apiKey(System.getenv("CREATEOS_API_KEY")).build();
    Sandbox sandbox =
        client.createSandbox(
            CreateSandboxRequest.builder("s-1vcpu-1gb").rootFileSystem("devbox:1").build());
    Checkpoint checkpoint = null;
    try {
      sandbox.shell("mkdir -p " + PATH + " && printf 'version one' > " + PATH + "/value");
      checkpoint = sandbox.checkpoints().create(new CreateCheckpointRequest(PATH, "before-update"));
      sandbox.shell("printf 'version two' > " + PATH + "/value");
      sandbox.checkpoints().restore(checkpoint.id());
      String contents = sandbox.shell("cat " + PATH + "/value").result().standardOutput();
      System.out.printf("restored %s: %s%n", checkpoint.id(), contents);
    } finally {
      try {
        if (checkpoint != null) {
          sandbox.checkpoints().delete(checkpoint.id());
        }
      } finally {
        sandbox.destroy();
      }
    }
  }
}
