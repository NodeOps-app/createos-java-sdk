package network.nodeops.createos;

import network.nodeops.createos.model.RunCommandResponse;

/** Indicates that a shell command completed unsuccessfully. */
public final class CommandException extends RuntimeException {
  private final RunCommandResponse response;

  /** Creates an exception retaining the complete command response. */
  public CommandException(String message, RunCommandResponse response) {
    super(message);
    this.response = response;
  }

  /** Returns the unsuccessful command response. */
  public RunCommandResponse response() {
    return response;
  }
}
