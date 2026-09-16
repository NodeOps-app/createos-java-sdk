package sh.createos;

import java.util.ArrayDeque;
import java.util.Deque;
import sh.createos.internal.NdjsonStream;
import sh.createos.model.CommandStreamEvent;
import sh.createos.model.CommandStreamFrame;

/** Incremental output from a streaming sandbox command. */
public final class CommandStream implements AutoCloseable {
  private final NdjsonStream<CommandStreamFrame> stream;
  private final Deque<CommandStreamEvent> queued = new ArrayDeque<>();

  CommandStream(NdjsonStream<CommandStreamFrame> stream) {
    this.stream = stream;
  }

  /** Returns the next event, or {@code null} when the stream has ended. */
  public CommandStreamEvent receive() {
    while (queued.isEmpty()) {
      CommandStreamFrame frame = stream.receive();
      if (frame == null) {
        return null;
      }
      if (frame.heartbeat()) {
        queued.add(new CommandStreamEvent(CommandStreamEvent.Type.HEARTBEAT, null, null, null));
      }
      if (frame.standardOutput() != null && !frame.standardOutput().isEmpty()) {
        queued.add(
            new CommandStreamEvent(
                CommandStreamEvent.Type.STDOUT, frame.standardOutput(), null, null));
      }
      if (frame.standardError() != null && !frame.standardError().isEmpty()) {
        queued.add(
            new CommandStreamEvent(
                CommandStreamEvent.Type.STDERR, frame.standardError(), null, null));
      }
      if (frame.errorMessage() != null && !frame.errorMessage().isEmpty()) {
        queued.add(
            new CommandStreamEvent(
                CommandStreamEvent.Type.ERROR, null, null, frame.errorMessage()));
      }
      if (frame.exitCode() != null) {
        queued.add(
            new CommandStreamEvent(CommandStreamEvent.Type.EXIT, null, frame.exitCode(), null));
      }
    }
    return queued.removeFirst();
  }

  @Override
  public void close() {
    stream.close();
  }
}
