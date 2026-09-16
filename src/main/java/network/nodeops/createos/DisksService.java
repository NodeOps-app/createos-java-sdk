package network.nodeops.createos;

import java.util.List;
import java.util.Map;
import network.nodeops.createos.internal.HttpTransport;
import network.nodeops.createos.model.CreateDiskRequest;
import network.nodeops.createos.model.CreateDiskRequest.DiskCredentials;
import network.nodeops.createos.model.Disk;
import network.nodeops.createos.model.PaginationOptions;

/** Account-level persistent disk registration operations. */
public final class DisksService {
  private final CreateOsClient client;

  DisksService(CreateOsClient client) {
    this.client = client;
  }

  /** Lists persistent disks registered by the caller. */
  public List<Disk> list(PaginationOptions options) {
    return client.fetchAll(
        "/v1/disks",
        options.offset() == 0 ? Map.of() : Map.of("offset", Integer.toString(options.offset())),
        options.limit(),
        false,
        Disk.class);
  }

  /** Registers an S3-compatible persistent disk. */
  public Disk create(CreateDiskRequest request) {
    return client
        .transport()
        .send("POST", "/v1/disks", Map.of(), request, RequestOptions.DEFAULT, false, Disk.class);
  }

  /** Returns a disk by identifier or user-scoped name. */
  public Disk get(String diskIdOrName) {
    return client
        .transport()
        .send("GET", path(diskIdOrName), Map.of(), null, RequestOptions.DEFAULT, false, Disk.class);
  }

  /** Deletes a disk registration without modifying bucket contents. */
  public boolean delete(String diskIdOrName) {
    Deleted response =
        client
            .transport()
            .send(
                "DELETE",
                path(diskIdOrName),
                Map.of(),
                null,
                RequestOptions.DEFAULT,
                false,
                Deleted.class);
    return response.deleted();
  }

  /** Replaces the write-only credentials stored for a disk. */
  public Disk rotateCredentials(String diskIdOrName, DiskCredentials credentials) {
    return client
        .transport()
        .send(
            "PATCH",
            path(diskIdOrName),
            Map.of(),
            Map.of("credentials", credentials),
            RequestOptions.DEFAULT,
            false,
            Disk.class);
  }

  private static String path(String diskIdOrName) {
    return "/v1/disks/" + HttpTransport.encodePathSegment(diskIdOrName);
  }

  private record Deleted(boolean deleted) {}
}
