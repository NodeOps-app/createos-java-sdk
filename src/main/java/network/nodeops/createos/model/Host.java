package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Public worker host projection. */
public record Host(
    String id,
    String status,
    @JsonProperty("free_mib") int freeMemoryMib,
    @JsonProperty("vm_count") int sandboxCount,
    @JsonProperty("rootfses") List<String> rootFileSystems) {}
