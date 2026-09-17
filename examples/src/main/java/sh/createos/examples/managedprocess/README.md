# Managed process lifecycle

Exercises process input, output, waiting, and termination. See [ManagedProcessExample.java](ManagedProcessExample.java) for the code.

From the repository root, [compile the examples and build the classpath](../../../../../../../README.md), then run:

```sh
export CREATEOS_API_KEY="your-api-key"
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  sh.createos.examples.managedprocess.ManagedProcessExample
```

This example makes live API calls and may create billable resources.
