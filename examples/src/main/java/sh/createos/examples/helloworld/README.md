# Hello world

Creates a sandbox, runs a command, and destroys it. See [HelloWorld.java](HelloWorld.java) for the code.

From the repository root, [compile the examples and build the classpath](../../../../../../../README.md), then run:

```sh
export CREATEOS_API_KEY="your-api-key"
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  sh.createos.examples.helloworld.HelloWorld
```

This example makes live API calls and may create billable resources.
