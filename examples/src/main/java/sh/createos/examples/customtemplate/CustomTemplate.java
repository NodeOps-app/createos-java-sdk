package sh.createos.examples.customtemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import sh.createos.CreateOsClient;
import sh.createos.RequestOptions;
import sh.createos.Sandbox;
import sh.createos.model.CreateSandboxRequest;
import sh.createos.model.CreateTemplateRequest;
import sh.createos.model.RunCommandRequest;
import sh.createos.model.Template;

/** Builds a Docker-enabled template and runs containers in a sandbox created from it. */
public final class CustomTemplate {
  private static final String DOCKERFILE =
      """
      FROM nodeops/sandbox:debian

      RUN apt-get update -qq \\
       && apt-get install -y --no-install-recommends curl ca-certificates \\
       && curl -fsSL https://get.docker.com | sh \\
       && rm -rf /var/lib/apt/lists/*
      """;

  private CustomTemplate() {}

  /** Runs the example. */
  public static void main(String[] arguments) throws InterruptedException {
    CreateOsClient client = CreateOsClient.builder().build();
    Template template =
        client
            .templates()
            .create(
                new CreateTemplateRequest(
                    "docker-ce-" + Instant.now().toEpochMilli(), DOCKERFILE, null));
    Sandbox sandbox = null;
    try {
      System.out.println("template: " + template.id());
      try (var logs =
          client
              .templates()
              .followLogs(
                  template.id(),
                  0,
                  new RequestOptions(Map.of(), Duration.ofMinutes(10), null, true))) {
        for (var event = logs.receive(); event != null; event = logs.receive()) {
          if (event.line() != null && !event.line().isEmpty()) {
            System.out.println(event.line());
          }
          if (event.isFinal()) {
            break;
          }
        }
      }
      waitUntilReady(client, template.id());
      sandbox =
          client.createSandbox(
              CreateSandboxRequest.builder("s-1vcpu-1gb").rootFileSystem(template.id()).build());
      sandbox.shell("nohup setsid dockerd > /var/log/dockerd.log 2>&1 &");
      waitForDocker(sandbox);
      printSuccessful(sandbox, RunCommandRequest.of("docker", "run", "--rm", "hello-world"));
      printSuccessful(
          sandbox,
          RunCommandRequest.of(
              "docker", "run", "--rm", "alpine", "sh", "-c", "echo hello from alpine"));
      printSuccessful(sandbox, RunCommandRequest.of("docker", "images"));
    } finally {
      if (sandbox != null) {
        sandbox.destroy();
      }
      client.templates().delete(template.id());
    }
  }

  private static void waitUntilReady(CreateOsClient client, String templateId)
      throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofMinutes(10).toNanos();
    while (System.nanoTime() < deadline) {
      String status = client.templates().get(templateId, false).status();
      if ("ready".equals(status)) {
        return;
      }
      if ("failed".equals(status)) {
        throw new IllegalStateException("template build failed");
      }
      Thread.sleep(Duration.ofSeconds(2).toMillis());
    }
    throw new IllegalStateException("template build timed out");
  }

  private static void waitForDocker(Sandbox sandbox) throws InterruptedException {
    for (int attempt = 0; attempt < 30; attempt++) {
      var response =
          sandbox.runCommand(
              RunCommandRequest.of("docker", "info", "--format", "{{.ServerVersion}}"));
      if (response.result().exitCode() == 0) {
        return;
      }
      Thread.sleep(Duration.ofSeconds(2).toMillis());
    }
    throw new IllegalStateException("dockerd did not start within 60 seconds");
  }

  private static void printSuccessful(Sandbox sandbox, RunCommandRequest request) {
    var response = sandbox.runCommand(request);
    if (response.result().exitCode() != 0) {
      throw new IllegalStateException(response.result().standardError());
    }
    System.out.println(response.result().standardOutput().trim());
  }
}
