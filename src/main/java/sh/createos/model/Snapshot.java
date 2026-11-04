package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Owned reusable sandbox snapshot. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Snapshot(
    String id,
    @JsonProperty("sandbox_id") String sandboxId,
    String name,
    SnapshotType type,
    SnapshotStatus status,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("ready_at") Instant readyAt,
    String shape,
    @JsonProperty("vcpu") int virtualCpu,
    @JsonProperty("mem_mib") int memoryMib,
    @JsonProperty("disk_mib") long diskMib,
    @JsonProperty("size_bytes") long sizeBytes) {}
