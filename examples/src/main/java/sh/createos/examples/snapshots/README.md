# Reusable snapshots

Capture a reusable disk snapshot, wait for it to become ready, and restore a
new sandbox from it.

```bash
export CREATEOS_API_KEY="..."
mvn verify
mvn dependency:build-classpath -Dmdep.outputFile=/tmp/createos-java-classpath
java -cp "target/classes:target/example-classes:$(cat /tmp/createos-java-classpath)" \
  sh.createos.examples.snapshots.Snapshots
```
