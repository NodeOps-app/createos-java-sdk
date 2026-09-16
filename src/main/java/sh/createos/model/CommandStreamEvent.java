package sh.createos.model;

/** A normalized event emitted by a streaming command. */
public record CommandStreamEvent(Type type, String data, Integer exitCode, String message) {
  /** Command stream event kind. */
  public enum Type {
    STDOUT,
    STDERR,
    EXIT,
    ERROR,
    HEARTBEAT
  }
}
