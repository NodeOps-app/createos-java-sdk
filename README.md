# CreateOS Java SDK

Launch an isolated cloud sandbox, run real commands, stream output, move files,
open a preview URL, and tear everything down from Java.

The SDK targets Java 17 and uses the JDK HTTP client. Public wire models are
immutable records, API failures remain inspectable through `ApiException`, and
the build enforces the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
with `google-java-format` and Checkstyle.

## Your first sandbox

The project is currently a development snapshot. Install it locally:

```sh
mvn install
```

Then add it to a Maven project:

```xml
<dependency>
  <groupId>network.nodeops</groupId>
  <artifactId>createos-java-sdk</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

The compile-checked [hello-world example](examples/src/main/java/network/nodeops/createos/examples/helloworld/HelloWorld.java)
creates a sandbox, runs a command, prints its output, and destroys the resource
in a `finally` block:

```text
Java says hello from x86_64
```

Do not commit a real API key to source control. Inject it through your
application's secret manager. `CreateOsClient.builder()` can configure the API
key, endpoint, default timeout, HTTP client, user agent, and retry policy.

When an API key is not provided explicitly, the builder reads
`CREATEOS_SANDBOX_API_KEY`. `CREATEOS_SANDBOX_BASE_URL` overrides the default
control-plane URL.

## Documentation

- [CreateOS Sandbox overview](https://nodeops.network/createos/docs/Sandbox/Overview)
  explains the sandbox model, lifecycle, networking, storage, and isolation.
- [CreateOS Sandbox documentation](https://nodeops.network/createos/docs)
  contains the REST API reference and product guides.
- [CreateOS TypeScript SDK](https://github.com/NodeOps-app/createos-sandbox-sdk)
  provides the same capabilities for JavaScript and TypeScript.
- [CreateOS Python SDK](https://github.com/NodeOps-app/createos-python-sdk)
  provides the same sandbox capabilities for Python applications.
- [CreateOS Go SDK](https://github.com/NodeOps-app/createos-go-sdk) is the
  behavioral reference for endpoint paths and wire contracts.
- [Runnable Java examples](#examples) cover commands, files, streaming,
  ingress, snapshots, networking, templates, managed processes, and desktops.
- [Contributing guide](CONTRIBUTING.md) documents development checks and commit
  conventions.
- [Security policy](SECURITY.md) explains private vulnerability reporting and
  the SDK security boundary.
- [Changelog](CHANGELOG.md) tracks release-visible changes.

## Stream output as it happens

Long-running commands do not need to disappear behind a buffered HTTP call.
The compile-checked [command-streaming example](examples/src/main/java/network/nodeops/createos/examples/commandstreaming/CommandStreaming.java)
uploads a Python program, consumes normalized stdout, stderr, exit, error, and
heartbeat events, and closes the stream with try-with-resources.

Closing a command or process stream closes its HTTP response body. Downloads,
screenshots, and template log streams must also be closed.

## Move files without shell escaping

`sandbox.files().upload(path, inputStream)` transfers bytes without shell
quoting. `download(path)` returns an `InputStream` that should be used with
try-with-resources. Both operations accept `RequestOptions` for long transfers.

The compile-checked [files-and-snapshots example](examples/src/main/java/network/nodeops/createos/examples/filesandsnapshots/FilesAndSnapshots.java)
uploads files, pauses a sandbox, creates a copy-on-write fork, resumes both
sandboxes, and verifies that their filesystems diverge.

## Keep a process alive after disconnecting

Managed processes are resources rather than fragile terminal sessions. The
compile-checked [managed-process example](examples/src/main/java/network/nodeops/createos/examples/managedprocess/ManagedProcessExample.java)
demonstrates:

- Pipe processes with standard input, output, and error streams
- Reconnectable output with sequence-based replay
- Interactive PTYs and terminal resize
- Waiting for a complete process tree
- Terminating long-running process trees

The primary operations are available from `sandbox.processes()`: `create`,
`list`, `get`, `connect`, `input`, `closeStandardInput`, `resize`, `signal`,
`waitFor`, and `delete`.

## Turn a service into a URL

Create a sandbox with ingress enabled, start a managed service, wait for its TCP
port, then call `sandbox.previewUrl(port)`. The compile-checked
[ingress-preview example](examples/src/main/java/network/nodeops/createos/examples/ingresspreview/IngressPreview.java)
starts a Python HTTP server and fetches its public response. Its relaxed TLS
trust policy is deliberately isolated to the example client because preview
ingress currently uses a self-signed certificate.

## Everything is already connected

Account-level services are initialized by `CreateOsClient`:

- `client.templates()`
- `client.networks()`
- `client.disks()`

Sandbox-level services are initialized with every created or retrieved handle:

- `sandbox.files()`
- `sandbox.processes()`
- `sandbox.computer().mouse()`
- `sandbox.computer().keyboard()`
- `sandbox.computer().windows()`
- `sandbox.computer().screens()`

The compile-checked [custom-template example](examples/src/main/java/network/nodeops/createos/examples/customtemplate/CustomTemplate.java)
builds a Docker-enabled root filesystem, follows build logs, creates a sandbox
from the finished template, and runs containers.

## Connect sandboxes on a private network

The compile-checked [private-network example](examples/src/main/java/network/nodeops/createos/examples/network/NetworkExample.java)
creates an overlay network, attaches a running sandbox, inspects membership,
and cleans up in reverse dependency order.

Use `client.networks()` for account-level network lifecycle and
`sandbox.attachNetwork` or `sandbox.detachNetwork` for membership.

## Control desktop workloads

The compile-checked [desktop example](examples/src/main/java/network/nodeops/createos/examples/desktop/Desktop.java)
waits for a graphical desktop, lists screens, captures and decodes a PNG,
moves the cursor, round-trips clipboard text, opens a URL, and creates a
temporary noVNC connection.

## Build an execution service

The compile-checked [execution-server example](examples/src/main/java/network/nodeops/createos/examples/executionserver/ExecutionServer.java)
exposes `POST /v1/execute` on localhost. Each request creates a fresh sandbox,
runs one command without shell interpolation, returns JSON, and destroys the
sandbox. It limits request bodies and concurrent executions. See the
[execution-server guide](examples/execution-server/README.md) for its request
format and security considerations.

## Lifecycle reads like the domain

`Sandbox` exposes `pause`, `resume`, `fork`, `destroy`, `resize`, and explicit
wait methods. Its latest server projection is cached safely. Lifecycle
mutations and `refresh()` update that projection, while `id()`, `name()`,
`status()`, `ipAddress()`, and `data()` provide thread-safe reads.

The files-and-snapshots example exercises pause, wait, fork, resume, and destroy
as one complete lifecycle.

## Errors stay inspectable

API failures throw `ApiException`, which exposes:

- HTTP status and service error code
- Server request identifier
- Request method and relative endpoint
- Bounded response body
- Response headers

Errors are unchecked so applications can handle them at the appropriate
boundary instead of wrapping every SDK call. Lifecycle wait exhaustion throws
`IllegalStateException` with the target state and timeout.

## Examples

Every example named here is compiled during `mvn verify`, checked with the
Google Java style rules, and covered by `ExamplesDocumentationTest`, which
fails when a linked source file or compiled example class is missing:

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

## API areas

- Sandbox creation, lookup, listing, lifecycle, forking, resize, ingress, and auto-pause
- Buffered and NDJSON/SSE command execution
- Streaming uploads and downloads
- Managed processes and PTYs with replay, input, signals, wait, and delete
- Egress, bandwidth, overlay networks, and persistent disks
- Custom templates and streaming build logs
- Desktop screens, noVNC connections, screenshots, mouse, keyboard, and windows
- Health, readiness, caller identity, shapes, root filesystems, and hosts
- In-sandbox self-pause and self-delete signals

## Development

Use Java 17 or newer and Maven 3.9 or newer:

```sh
make format
make check
make test
```

Commits follow Conventional Commits. See [CONTRIBUTING.md](CONTRIBUTING.md) for
accepted types and review expectations. CI compiles the SDK and examples, runs
JUnit tests, verifies Google Java formatting, and requires zero Checkstyle
warnings.

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
