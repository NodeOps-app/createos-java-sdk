package sh.createos.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Incremental newline-delimited JSON and SSE data-line decoder. */
public final class NdjsonStream<T> implements AutoCloseable {
  private final BufferedReader reader;
  private final ObjectMapper objectMapper;
  private final Class<T> eventType;
  private boolean closed;

  /** Creates a stream that owns {@code input}. */
  public NdjsonStream(InputStream input, ObjectMapper objectMapper, Class<T> eventType) {
    this.reader =
        new BufferedReader(
            new InputStreamReader(Objects.requireNonNull(input), StandardCharsets.UTF_8));
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.eventType = Objects.requireNonNull(eventType);
  }

  /** Returns the next event, or {@code null} after the stream is exhausted. */
  public T receive() {
    if (closed) {
      return null;
    }
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()
            || line.startsWith(":")
            || line.startsWith("event:")
            || line.startsWith("id:")
            || line.startsWith("retry:")) {
          continue;
        }
        if (line.startsWith("data:")) {
          line = line.substring("data:".length()).trim();
          if (line.isEmpty()) {
            continue;
          }
        }
        return objectMapper.readValue(line, eventType);
      }
      close();
      return null;
    } catch (IOException exception) {
      closeQuietly();
      throw new IllegalStateException("Failed to read event stream", exception);
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      reader.close();
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to close event stream", exception);
    }
  }

  private void closeQuietly() {
    try {
      close();
    } catch (RuntimeException ignored) {
      // Preserve the stream decoding failure.
    }
  }
}
