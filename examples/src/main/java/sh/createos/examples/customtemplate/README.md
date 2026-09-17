# Custom template

Builds a template and starts a sandbox from it. See [CustomTemplate.java](CustomTemplate.java) for the code.

From the repository root, [compile the examples and build the classpath](../../../../../../../README.md), then run:

```sh
export CREATEOS_API_KEY="your-api-key"
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  sh.createos.examples.customtemplate.CustomTemplate
```

This example makes live API calls and may create billable resources.
