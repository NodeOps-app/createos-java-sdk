# Desktop and noVNC

Exercises desktop automation and a noVNC connection. See [Desktop.java](Desktop.java) for the code.

From the repository root, [compile the examples and build the classpath](../../../../../../../README.md), then run:

```sh
export CREATEOS_API_KEY="your-api-key"
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  sh.createos.examples.desktop.Desktop
```

This example makes live API calls and may create billable resources.
