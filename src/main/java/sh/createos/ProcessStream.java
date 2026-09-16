package sh.createos;

import java.util.Base64;
import sh.createos.internal.NdjsonStream;
import sh.createos.model.ProcessStreamEvent;
import sh.createos.model.ProcessStreamFrame;

/** Replayable output stream for a managed process. */
public final class ProcessStream implements AutoCloseable {
  private final NdjsonStream<ProcessStreamFrame> stream;

  ProcessStream(NdjsonStream<ProcessStreamFrame> stream) {
    this.stream = stream;
  }

  /** Returns the next process event, or {@code null} when the stream ends. */
  public ProcessStreamEvent receive() {
    ProcessStreamFrame frame = stream.receive();
    if (frame == null) {
      return null;
    }
    byte[] data =
        frame.dataBase64() == null || frame.dataBase64().isEmpty()
            ? new byte[0]
            : Base64.getDecoder().decode(frame.dataBase64());
    return new ProcessStreamEvent(
        frame.type(),
        frame.sequence(),
        frame.stream(),
        data,
        frame.exitCode(),
        frame.signal(),
        frame.errorMessage(),
        frame.oldestAvailableSequence());
  }

  @Override
  public void close() {
    stream.close();
  }
}
