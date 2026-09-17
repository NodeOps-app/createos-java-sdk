# Private overlay network

Creates a network, attaches a sandbox, and verifies membership. See [NetworkExample.java](NetworkExample.java) for the code.

From the repository root, [compile the examples and build the classpath](../../../../../../../README.md), then run:

```sh
export CREATEOS_API_KEY="your-api-key"
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  sh.createos.examples.network.NetworkExample
```

This example makes live API calls and may create billable resources.
