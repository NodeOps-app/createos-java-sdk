# Files and snapshots

Uploads a file, forks a sandbox, and checks inherited file state. See [FilesAndSnapshots.java](FilesAndSnapshots.java) for the code.

From the repository root, [compile the examples and build the classpath](../../../../../../../README.md), then run:

```sh
export CREATEOS_API_KEY="your-api-key"
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  sh.createos.examples.filesandsnapshots.FilesAndSnapshots
```

This example makes live API calls and may create billable resources.
