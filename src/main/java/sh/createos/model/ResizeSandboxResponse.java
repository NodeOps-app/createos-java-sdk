package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Sandbox disk size after a grow operation. */
public record ResizeSandboxResponse(String id, @JsonProperty("disk_mib") long diskMib) {}
