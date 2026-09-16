package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Persistent disk mount attached to a sandbox. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DiskAttachment(
    @JsonProperty("disk_id") String diskId,
    @JsonProperty("mount_path") String mountPath,
    @JsonProperty("sub_path") String subPath) {}
