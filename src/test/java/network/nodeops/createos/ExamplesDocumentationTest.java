package network.nodeops.createos;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Keeps README example references synchronized with compile-checked programs. */
final class ExamplesDocumentationTest {
  private static final List<Example> EXAMPLES =
      List.of(
          example("helloworld", "HelloWorld"),
          example("executionserver", "ExecutionServer"),
          example("commandstreaming", "CommandStreaming"),
          example("filesandsnapshots", "FilesAndSnapshots"),
          example("ingresspreview", "IngressPreview"),
          example("network", "NetworkExample"),
          example("customtemplate", "CustomTemplate"),
          example("managedprocess", "ManagedProcessExample"),
          example("desktop", "Desktop"));

  @Test
  void everyDocumentedExampleHasSourceAndCompiledClass() throws Exception {
    Path project = Path.of("").toAbsolutePath();
    String readme = Files.readString(project.resolve("README.md"));
    Path exampleClasses = project.resolve("target/example-classes");

    try (var loader =
        new URLClassLoader(
            new java.net.URL[] {exampleClasses.toUri().toURL()}, getClass().getClassLoader())) {
      for (Example example : EXAMPLES) {
        assertTrue(
            readme.contains("(" + example.source() + ")"),
            () -> "README does not link " + example.source());
        assertTrue(
            Files.isRegularFile(project.resolve(example.source())),
            () -> "example source is missing: " + example.source());
        assertDoesNotThrow(
            () -> Class.forName(example.className(), false, loader),
            () -> "example was not compiled: " + example.className());
      }
    }
  }

  private static Example example(String packageName, String simpleName) {
    String className = "network.nodeops.createos.examples." + packageName + "." + simpleName;
    String source = "examples/src/main/java/" + className.replace('.', '/') + ".java";
    return new Example(source, className);
  }

  private record Example(String source, String className) {}
}
