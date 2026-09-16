package sh.createos.model;

/** Decoded managed-process output event. */
public record ProcessStreamEvent(
    String type,
    long sequence,
    String stream,
    byte[] data,
    Integer exitCode,
    String signal,
    String errorMessage,
    long oldestAvailableSequence) {
  /** Defensively copies binary event data. */
  public ProcessStreamEvent {
    data = data == null ? new byte[0] : data.clone();
  }

  @Override
  public byte[] data() {
    return data.clone();
  }
}
