package sh.createos;

import java.util.List;
import java.util.Map;
import sh.createos.internal.HttpTransport;
import sh.createos.model.Network;
import sh.createos.model.PaginationOptions;

/** Account-level overlay network operations. */
public final class NetworksService {
  private final CreateOsClient client;

  NetworksService(CreateOsClient client) {
    this.client = client;
  }

  /** Lists overlay networks owned by the caller. */
  public List<Network> list(PaginationOptions options) {
    return client.fetchAll(
        "/v1/networks",
        options.offset() == 0 ? Map.of() : Map.of("offset", Integer.toString(options.offset())),
        options.limit(),
        false,
        Network.class);
  }

  /** Creates an overlay network. */
  public Network create(String name) {
    return client
        .transport()
        .send(
            "POST",
            "/v1/networks",
            Map.of(),
            Map.of("name", name),
            RequestOptions.DEFAULT,
            false,
            Network.class);
  }

  /** Returns an overlay network by identifier. */
  public Network get(String networkId) {
    return client
        .transport()
        .send("GET", path(networkId), Map.of(), null, RequestOptions.DEFAULT, false, Network.class);
  }

  /** Deletes an overlay network. */
  public void delete(String networkId) {
    client
        .transport()
        .send("DELETE", path(networkId), Map.of(), null, RequestOptions.DEFAULT, false, Void.class);
  }

  private static String path(String networkId) {
    return "/v1/networks/" + HttpTransport.encodePathSegment(networkId);
  }
}
