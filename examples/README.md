# CreateOS Java SDK examples

These programs mirror the runnable examples in `createos-go-sdk`. They are
compiled during `mvn verify`, but are not included in the SDK JAR.
The SDK is [published on Maven Central](https://central.sonatype.com/artifact/sh.createos/createos-java-sdk);
these examples compile against the source checkout.

Set your API key and compile everything first:

```sh
export CREATEOS_API_KEY="your-api-key"
mvn verify
```

Run an example with the SDK dependencies on the classpath:

```sh
mvn dependency:build-classpath -Dmdep.outputFile=/tmp/createos-java-classpath
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  sh.createos.examples.helloworld.HelloWorld
```

Replace the final class with one of these:

| Example | Java main class |
| --- | --- |
| `hello-world` | `sh.createos.examples.helloworld.HelloWorld` |
| `command-streaming` | `sh.createos.examples.commandstreaming.CommandStreaming` |
| `custom-template` | `sh.createos.examples.customtemplate.CustomTemplate` |
| `desktop` | `sh.createos.examples.desktop.Desktop` |
| `execution-server` | `sh.createos.examples.executionserver.ExecutionServer` |
| `files-and-snapshots` | `sh.createos.examples.filesandsnapshots.FilesAndSnapshots` |
| `ingress-preview` | `sh.createos.examples.ingresspreview.IngressPreview` |
| `managed-process` | `sh.createos.examples.managedprocess.ManagedProcessExample` |
| `network` | `sh.createos.examples.network.NetworkExample` |

Examples create billable resources. Their `finally` blocks make a best effort
to destroy sandboxes and delete temporary account resources.

See [execution server README](src/main/java/sh/createos/examples/executionserver/README.md) for the HTTP API
request format and security considerations for that example.
