# Checkpoints

Capture a directory, modify it, restore its earlier contents, and remove the
checkpoint.

```sh
export CREATEOS_API_KEY="..."
mvn verify
mvn dependency:build-classpath -Dmdep.outputFile=/tmp/createos-java-classpath
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  sh.createos.examples.checkpoints.Checkpoints
```

The example creates live resources and cleans them up when it finishes. Stop
all writers before creating or restoring a checkpoint.
