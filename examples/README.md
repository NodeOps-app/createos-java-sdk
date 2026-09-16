# CreateOS Java SDK examples

These programs mirror the runnable examples in `createos-go-sdk`. They are
compiled during `mvn verify`, but are not included in the SDK JAR.

Set your API key and compile everything first:

```sh
export CREATEOS_API_KEY="your-api-key"
mvn verify
```

Run an example with the SDK dependencies on the classpath:

```sh
mvn dependency:build-classpath -Dmdep.outputFile=/tmp/createos-java-classpath
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  network.nodeops.createos.examples.helloworld.HelloWorld
```

Replace the final class with one of these:

| Go example | Java main class |
| --- | --- |
| `hello-world` | `network.nodeops.createos.examples.helloworld.HelloWorld` |
| `command-streaming` | `network.nodeops.createos.examples.commandstreaming.CommandStreaming` |
| `custom-template` | `network.nodeops.createos.examples.customtemplate.CustomTemplate` |
| `desktop` | `network.nodeops.createos.examples.desktop.Desktop` |
| `execution-server` | `network.nodeops.createos.examples.executionserver.ExecutionServer` |
| `files-and-snapshots` | `network.nodeops.createos.examples.filesandsnapshots.FilesAndSnapshots` |
| `ingress-preview` | `network.nodeops.createos.examples.ingresspreview.IngressPreview` |
| `managed-process` | `network.nodeops.createos.examples.managedprocess.ManagedProcessExample` |
| `network` | `network.nodeops.createos.examples.network.NetworkExample` |

Examples create billable resources. Their `finally` blocks make a best effort
to destroy sandboxes and delete temporary account resources.

See [execution-server/README.md](execution-server/README.md) for the HTTP API
request format and security considerations for that example.
