# CreateOS Java SDK

Launch an isolated cloud sandbox, run real commands, stream output, move files,
open a preview URL, and tear everything down from Java.

## Your first sandbox

Version [`0.1.0`](https://github.com/NodeOps-app/createos-java-sdk/releases/tag/v0.1.0)
is published on GitHub Packages. Add the repository and dependency to your
project's `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/NodeOps-app/createos-java-sdk</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>network.nodeops</groupId>
    <artifactId>createos-java-sdk</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```

GitHub Packages requires authentication to download Maven packages, including
public packages. Set `GITHUB_USERNAME` to your GitHub username and
`GITHUB_TOKEN` to a personal access token (classic) with `read:packages` in
your environment or secret manager. Do not commit either value. Configure Maven
to read them from `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>${env.GITHUB_USERNAME}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
```

For local development from this checkout, run `mvn install` instead of
configuring GitHub Packages.

The compile-checked [hello-world example](examples/src/main/java/network/nodeops/createos/examples/helloworld/HelloWorld.java)
creates a sandbox, runs a command, prints its output, and always destroys the
resource:

```java
package network.nodeops.createos.examples.helloworld;

import network.nodeops.createos.CreateOsClient;
import network.nodeops.createos.Sandbox;
import network.nodeops.createos.model.CreateSandboxRequest;
import network.nodeops.createos.model.RunCommandRequest;

public final class HelloWorld {
  private HelloWorld() {}

  public static void main(String[] arguments) {
    CreateOsClient client = CreateOsClient.builder().build();
    Sandbox sandbox =
        client.createSandbox(
            CreateSandboxRequest.builder("s-4vcpu-4gb")
                .rootFileSystem("devbox:1")
                .build());
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
```

```text
Java says hello from x86_64
```

`CreateOsClient.builder().apiKey(apiKey)` configures authentication explicitly.
Do not commit a real API key to source control; inject it through your
application's secret manager. Additional builder methods configure the
endpoint, default timeout, HTTP client, user agent, and retry policy:

```java
CreateOsClient client =
    CreateOsClient.builder()
        .apiKey(apiKey)
        .baseUri(URI.create("http://localhost:8080"))
        .timeout(Duration.ofSeconds(30))
        .build();
```

As an optional alternative, `CreateOsClient.builder().build()` reads
`CREATEOS_SANDBOX_API_KEY`. `CREATEOS_SANDBOX_BASE_URL` overrides the default
control-plane URL. Explicit builder values take precedence.

The SDK targets Java 17 and uses the JDK HTTP client. Public wire models are
immutable records, API failures remain inspectable through `ApiException`, and
the build enforces the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

## Documentation

- [CreateOS Sandbox overview](https://nodeops.network/createos/docs/Sandbox/Overview)
  explains the sandbox model, lifecycle, networking, storage, and isolation.
- [CreateOS Sandbox documentation](https://nodeops.network/createos/docs)
  contains the REST API reference and product guides.
- Javadocs are generated with `mvn javadoc:javadoc` and published as an
  attached artifact by `mvn verify`.
- [Runnable examples](#examples) cover commands, files, streaming, ingress,
  snapshots, networking, templates, managed processes, and desktop use.
- [Contributing guide](CONTRIBUTING.md) documents development checks and commit
  conventions.
- [Security policy](SECURITY.md) explains private vulnerability reporting and
  the SDK security boundary.
- [Changelog](CHANGELOG.md) tracks release-visible changes.

### SDKs

- [TypeScript](https://github.com/NodeOps-app/createos-sandbox-sdk)
- [Python](https://github.com/NodeOps-app/createos-python-sdk)
- [Go](https://github.com/NodeOps-app/createos-go-sdk)
- [C#](https://github.com/NodeOps-app/createos-csharp-sdk)
- [Java](https://github.com/NodeOps-app/createos-java-sdk)
- [Rust](https://github.com/NodeOps-app/createos-rust-sdk)

## Stream output as it happens

Long-running commands do not need to disappear behind a buffered HTTP call:

```java
try (var stream =
    sandbox.streamCommand(
        RunCommandRequest.of(
            "sh", "-c", "for n in 1 2 3; do echo step $n; sleep 1; done"))) {
  for (var event = stream.receive(); event != null; event = stream.receive()) {
    switch (event.type()) {
      case STDOUT -> System.out.print(event.data());
      case STDERR -> System.err.print(event.data());
      case EXIT -> System.out.println("exit code: " + event.exitCode());
      case ERROR -> System.err.println(event.message());
      case HEARTBEAT -> {}
    }
  }
}
```

The compile-checked [command-streaming example](examples/src/main/java/network/nodeops/createos/examples/commandstreaming/CommandStreaming.java)
uploads a Python program and consumes normalized stdout, stderr, exit, error,
and heartbeat events. Closing a command or process stream closes its HTTP
response body. Downloads, screenshots, and template log streams must also be
closed.

## Move files without shell escaping

```java
byte[] configuration = "{\"mode\":\"production\"}".getBytes(StandardCharsets.UTF_8);
sandbox.files().upload("/workspace/config.json", new ByteArrayInputStream(configuration));

try (InputStream file = sandbox.files().download("/workspace/config.json")) {
  String contents = new String(file.readAllBytes(), StandardCharsets.UTF_8);
  System.out.println(contents);
}
```

For large transfers, override the timeout for that operation without changing
the client's default timeout:

```java
RequestOptions transferOptions =
    new RequestOptions(Map.of(), Duration.ofMinutes(30), null, false);

sandbox.files().upload(remotePath, source, transferOptions);
try (InputStream file = sandbox.files().download(remotePath, transferOptions)) {
  file.transferTo(destination);
}
```

The timeout covers the complete transfer, including reading the downloaded
body. Uploads are not retried because an arbitrary `InputStream` may not be
safe to replay after a partial write.

The compile-checked [files-and-snapshots example](examples/src/main/java/network/nodeops/createos/examples/filesandsnapshots/FilesAndSnapshots.java)
uploads files, pauses a sandbox, creates a copy-on-write fork, resumes both
sandboxes, and verifies that their filesystems diverge.

## Keep a process alive after disconnecting

Managed processes are resources rather than fragile terminal sessions. Start
one, reconnect from its output sequence, send input or signals, and wait for
either the leader or its complete process tree:

```java
ManagedProcess process =
    sandbox
        .processes()
        .create(
            new CreateProcessRequest(
                "python3",
                List.of("-m", "http.server", "8080"),
                null,
                Map.of(),
                null));

ManagedProcess completed =
    sandbox
        .processes()
        .waitFor(process.processId(), "tree", Duration.ofSeconds(30));
```

The compile-checked [managed-process example](examples/src/main/java/network/nodeops/createos/examples/managedprocess/ManagedProcessExample.java)
also demonstrates output replay, standard input, interactive PTYs, terminal
resize, signals, and process-tree deletion.

## Turn a service into a URL

Create with ingress enabled, wait for the server to listen, then ask the
sandbox for its public URL:

```java
Sandbox sandbox =
    client.createSandbox(
        CreateSandboxRequest.builder("s-4vcpu-4gb")
            .rootFileSystem("devbox:1")
            .ingressEnabled(true)
            .build());

sandbox
    .processes()
    .create(
        new CreateProcessRequest(
            "python3",
            List.of("-m", "http.server", "8080", "--bind", "0.0.0.0"),
            null,
            Map.of(),
            null));

sandbox.waitForPort("127.0.0.1", 8080, Duration.ofSeconds(15));
URI previewUrl = sandbox.previewUrl(8080);
System.out.println(previewUrl);
```

The compile-checked [ingress-preview example](examples/src/main/java/network/nodeops/createos/examples/ingresspreview/IngressPreview.java)
runs the complete flow and fetches the public response.

## Everything is already connected

Account-level services are initialized by `CreateOsClient`:

```java
TemplatesService templates = client.templates();
NetworksService networks = client.networks();
DisksService disks = client.disks();

List<Template> customTemplates = templates.list(PaginationOptions.ALL);
System.out.printf(
    "%d templates ready; networks=%s disks=%s%n",
    customTemplates.size(), networks.getClass().getSimpleName(), disks.getClass().getSimpleName());
```

Sandbox-level services are initialized when a handle is created or retrieved:

```java
sandbox.files();
sandbox.processes();
sandbox.computer().mouse();
sandbox.computer().keyboard();
sandbox.computer().windows();
sandbox.computer().screens();
```

The compile-checked [custom-template example](examples/src/main/java/network/nodeops/createos/examples/customtemplate/CustomTemplate.java)
builds a Docker-enabled root filesystem, follows build logs, creates a sandbox
from the finished template, and runs containers.

## Connect sandboxes on a private network

Create an overlay network, attach a running sandbox, and inspect the resulting
membership. Cleanup runs in reverse dependency order:

```java
Network network = client.networks().create("agent-mesh");
try {
  sandbox.attachNetwork(network.id());
  try {
    Network connected = client.networks().get(network.id());
    for (Network.Member member : connected.members()) {
      System.out.printf(
          "sandbox=%s private-ip=%s status=%s%n",
          member.sandboxId(), member.ipAddress(), member.status());
    }
  } finally {
    sandbox.detachNetwork(network.id());
  }
} finally {
  client.networks().delete(network.id());
}
```

The compile-checked [private-network example](examples/src/main/java/network/nodeops/createos/examples/network/NetworkExample.java)
runs this complete lifecycle.

## Lifecycle reads like the domain

```java
sandbox.pause();
sandbox.waitUntilPaused(Duration.ofMinutes(2));

Sandbox clone = sandbox.fork(ForkSandboxRequest.DEFAULT);
try {
  sandbox.resume();
  sandbox.waitUntilRunning(Duration.ofMinutes(2));
} finally {
  clone.destroy();
  sandbox.destroy();
}
```

The `Sandbox` handle caches the latest server projection safely. Lifecycle
mutations and `refresh()` update it, while `id()`, `name()`, `status()`,
`ipAddress()`, and `data()` provide thread-safe reads.

## Errors stay inspectable

```java
try {
  sandbox.refresh();
} catch (ApiException exception) {
  System.err.printf(
      "HTTP %d, code=%d, request=%s%n",
      exception.statusCode(), exception.serviceCode(), exception.requestId());
}
```

`ApiException` also exposes the request method, relative endpoint, bounded
response body, and response headers. Errors are unchecked so applications can
handle them at the appropriate boundary. Lifecycle wait exhaustion throws
`IllegalStateException` with the target state and timeout.

## Examples

Runnable examples live under [`examples/`](examples/). Every example listed
here is compiled during `mvn verify`, checked with Google Java style, and
validated by `ExamplesDocumentationTest`:

- [Hello world](examples/src/main/java/network/nodeops/createos/examples/helloworld/HelloWorld.java)
- [HTTP execution server](examples/src/main/java/network/nodeops/createos/examples/executionserver/ExecutionServer.java)
- [Command streaming](examples/src/main/java/network/nodeops/createos/examples/commandstreaming/CommandStreaming.java)
- [Files and snapshots](examples/src/main/java/network/nodeops/createos/examples/filesandsnapshots/FilesAndSnapshots.java)
- [Ingress preview](examples/src/main/java/network/nodeops/createos/examples/ingresspreview/IngressPreview.java)
- [Private overlay network](examples/src/main/java/network/nodeops/createos/examples/network/NetworkExample.java)
- [Custom template](examples/src/main/java/network/nodeops/createos/examples/customtemplate/CustomTemplate.java)
- [Managed process lifecycle](examples/src/main/java/network/nodeops/createos/examples/managedprocess/ManagedProcessExample.java)
- [Desktop and noVNC](examples/src/main/java/network/nodeops/createos/examples/desktop/Desktop.java)

Run commands are documented in the [examples index](examples/README.md).
Examples are compiled into `target/example-classes` and excluded from the SDK
JAR. Live execution is opt-in because it creates real CreateOS resources.

## Development

Use Java 17 or newer and Maven 3.9 or newer:

```sh
mvn install
make format
make check
make test
```

Commits follow Conventional Commits and are validated locally and in pull
requests. See [CONTRIBUTING.md](CONTRIBUTING.md) for accepted types and
examples.

CI compiles the SDK and every example on Java 17, 21, and 25; runs JUnit and
the coverage gate; verifies Google Java formatting; requires zero Checkstyle
warnings; and builds source and Javadoc JARs.

## Releases

An annotated `vMAJOR.MINOR.PATCH` tag on a commit in `main` triggers the
[release workflow](.github/workflows/release.yml). It requires the tag to match
the version in `pom.xml`, verifies Java 17, 21, and 25, publishes the Maven
package with source and Javadoc JARs to GitHub Packages, and creates a GitHub
Release with the same artifacts. The workflow uses the repository's
`GITHUB_TOKEN`; no personal token is needed for publishing from Actions.

For the first release, push the committed version bump before tagging:

```sh
git tag -a v0.1.0 -m "Release 0.1.0"
git push origin v0.1.0
```

## Package layout

```text
src/main/java/network/nodeops/createos/          client and resource services
src/main/java/network/nodeops/createos/model/    immutable public contracts
src/main/java/network/nodeops/createos/internal/ transport and stream decoding
examples/                                        runnable, compile-checked programs
```

## About CreateOS

[CreateOS](https://createos.sh) is an execution and governance platform for AI
agents and applications. Learn more about isolated Firecracker-based workloads
on the [CreateOS Sandbox product page](https://createos.sh/products/sandbox).

## License

This SDK is available under the [MIT License](LICENSE).
