# Ingress preview

Serves a page from a sandbox through a preview URL. See [IngressPreview.java](IngressPreview.java) for the code.

From the repository root, [compile the examples and build the classpath](../../../../../../../README.md), then run:

```sh
export CREATEOS_API_KEY="your-api-key"
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  sh.createos.examples.ingresspreview.IngressPreview
```

This example makes live API calls and may create billable resources.
