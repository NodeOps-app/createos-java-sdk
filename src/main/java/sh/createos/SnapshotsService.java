package sh.createos;

import java.util.List;
import java.util.Map;
import sh.createos.internal.HttpTransport;
import sh.createos.model.PaginationOptions;
import sh.createos.model.Snapshot;
import sh.createos.model.SnapshotDeletedResponse;

/** Account-level reusable sandbox snapshot operations. */
public final class SnapshotsService {
  private final CreateOsClient client;

  SnapshotsService(CreateOsClient client) {
    this.client = client;
  }

  /** Lists reusable snapshots owned by the caller. */
  public List<Snapshot> list(PaginationOptions options) {
    return client.fetchAll(
        "/v1/snapshots",
        options.offset() == 0 ? Map.of() : Map.of("offset", Integer.toString(options.offset())),
        options.limit(),
        false,
        Snapshot.class);
  }

  /** Returns an owned reusable snapshot by identifier. */
  public Snapshot get(String snapshotId) {
    return client
        .transport()
        .send(
            "GET", path(snapshotId), Map.of(), null, RequestOptions.DEFAULT, false, Snapshot.class);
  }

  /** Removes an owned ready or failed snapshot from API listings. */
  public SnapshotDeletedResponse delete(String snapshotId) {
    return client
        .transport()
        .send(
            "DELETE",
            path(snapshotId),
            Map.of(),
            null,
            RequestOptions.DEFAULT,
            false,
            SnapshotDeletedResponse.class);
  }

  private static String path(String snapshotId) {
    return "/v1/snapshots/" + HttpTransport.encodePathSegment(snapshotId);
  }
}
