package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import network.nodeops.createos.model.Disk.DiskConfig;

/** Persistent disk attachment visible inside a sandbox. */
public record SandboxDisk(
    @JsonProperty("disk_id") String diskId,
    String name,
    String kind,
    DiskConfig config,
    @JsonProperty("mount_path") String mountPath,
    @JsonProperty("sub_path") String subPath,
    @JsonProperty("mount_status") String mountStatus,
    @JsonProperty("mount_error") String mountError) {}
