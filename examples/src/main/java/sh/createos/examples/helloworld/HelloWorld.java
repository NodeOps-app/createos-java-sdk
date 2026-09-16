package sh.createos.examples.helloworld;

import sh.createos.CreateOsClient;
import sh.createos.Sandbox;
import sh.createos.model.CreateSandboxRequest;
import sh.createos.model.RunCommandRequest;

/** Creates a sandbox, runs a command, and destroys the sandbox. */
public final class HelloWorld {
  private HelloWorld() {}

  /** Runs the example. */
  public static void main(String[] arguments) {
    CreateOsClient client =
        CreateOsClient.builder().apiKey(System.getenv("CREATEOS_API_KEY")).build();
    Sandbox sandbox =
        client.createSandbox(
            CreateSandboxRequest.builder("s-4vcpu-4gb").rootFileSystem("devbox:1").build());
    try {
      var response =
          sandbox.runCommand(
              RunCommandRequest.of(
                  "sh", "-c", "printf 'Java says hello from %s\\n' \"$(uname -m)\""));
      System.out.print(response.result().standardOutput());
    } finally {
      sandbox.destroy();
    }
  }
}
