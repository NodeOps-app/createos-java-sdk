package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Resolved placement and boot timing returned when a sandbox is created. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateSandboxResponse(
    String id,
    SandboxStatus status,
    String name,
    @JsonProperty("ip") String ipAddress,
    String shape,
    @JsonProperty("rootfs") String rootFileSystem,
    @JsonProperty("vcpu") int virtualCpu,
    @JsonProperty("mem_mib") int memoryMib,
    @JsonProperty("disk_mib") long diskMib,
    @JsonProperty("spawn_ms") double spawnMilliseconds,
    @JsonProperty("egress") List<String> egressRules,
    @JsonProperty("bandwidth_quota_bytes") long bandwidthQuotaBytes,
    @JsonProperty("ingress_url_template") String ingressUrlTemplate) {}
