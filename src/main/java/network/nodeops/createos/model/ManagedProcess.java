package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/** Server projection of a persistent process. */
public record ManagedProcess(
    @JsonProperty("process_id") String processId,
    String kind,
    @JsonProperty("pid") int operatingSystemProcessId,
    String state,
    @JsonProperty("leader_exited") boolean leaderExited,
    @JsonProperty("tree_exited") boolean treeExited,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("finished_at") Instant finishedAt,
    @JsonProperty("exit_code") Integer exitCode,
    String signal,
    @JsonProperty("cmd") String command,
    @JsonProperty("args") List<String> arguments,
    @JsonProperty("cwd") String workingDirectory,
    Foreground foreground,
    OutputWindow output) {
  /** Foreground command in a PTY. */
  public record Foreground(
      @JsonProperty("pid") int operatingSystemProcessId,
      @JsonProperty("cmd") String command,
      @JsonProperty("args") List<String> arguments) {}

  /** Retained process-output sequence bounds. */
  public record OutputWindow(
      @JsonProperty("oldest_seq") long oldestSequence,
      @JsonProperty("newest_seq") long newestSequence,
      long bytes) {}
}
