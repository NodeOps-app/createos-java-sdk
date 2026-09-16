package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/** Account-level overlay network. */
public record Network(
    String id,
    String name,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("member_count") int memberCount,
    List<Member> members) {
  /** Sandbox membership in an overlay network. */
  public record Member(
      @JsonProperty("sandbox_id") String sandboxId,
      String status,
      @JsonProperty("ip") String ipAddress,
      String name) {}
}
