package sh.createos;

import java.util.List;
import java.util.Map;
import sh.createos.internal.HttpTransport;
import sh.createos.model.Checkpoint;
import sh.createos.model.CheckpointRestoreResponse;
import sh.createos.model.CreateCheckpointRequest;

/** Local checkpoint operations for one running sandbox. */
public final class CheckpointsService {
  private final Sandbox sandbox;

  CheckpointsService(Sandbox sandbox) {
    this.sandbox = sandbox;
  }

  /** Captures an eligible absolute directory. */
  public Checkpoint create(CreateCheckpointRequest request) {
    return sandbox
        .transport()
        .send(
            "POST",
            path("", ""),
            Map.of(),
            request,
            RequestOptions.DEFAULT,
            false,
            Checkpoint.class);
  }

  /** Returns checkpoints oldest first. */
  public List<Checkpoint> list() {
    return sandbox
        .transport()
        .sendList("GET", path("", ""), Map.of(), RequestOptions.DEFAULT, false, Checkpoint.class);
  }

  /** Restores a checkpoint at its original directory path. */
  public CheckpointRestoreResponse restore(String checkpointId) {
    return sandbox
        .transport()
        .send(
            "POST",
            path(checkpointId, "/restore"),
            Map.of(),
            null,
            RequestOptions.DEFAULT,
            false,
            CheckpointRestoreResponse.class);
  }

  /** Deletes a checkpoint record while retaining referenced layers. */
  public void delete(String checkpointId) {
    sandbox
        .transport()
        .send(
            "DELETE",
            path(checkpointId, ""),
            Map.of(),
            null,
            RequestOptions.DEFAULT,
            false,
            Void.class);
  }

  private String path(String checkpointId, String suffix) {
    String path = sandbox.path("/checkpoints");
    if (!checkpointId.isEmpty()) {
      path += "/" + HttpTransport.encodePathSegment(checkpointId);
    }
    return path + suffix;
  }
}
