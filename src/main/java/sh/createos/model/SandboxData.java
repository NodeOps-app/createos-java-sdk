package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Complete server projection of a sandbox. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxData(
    String id,
    SandboxStatus status,
    @JsonProperty("ip") String ipAddress,
    @JsonProperty("vcpu") int virtualCpu,
    @JsonProperty("mem_mib") int memoryMib,
    @JsonProperty("disk_mib") long diskMib,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("ingress_enabled") boolean ingressEnabled,
    @JsonProperty("ingress_url_template") String ingressUrlTemplate,
    String name,
    @JsonProperty("running_at") Instant runningAt,
    @JsonProperty("destroyed_at") Instant destroyedAt,
    @JsonProperty("spawn_ms") double spawnMilliseconds,
    String shape,
    @JsonProperty("rootfs") String rootFileSystem,
    String region,
    @JsonProperty("egress") List<String> egressRules,
    @JsonProperty("envs") List<String> environmentVariables,
    @JsonProperty("ssh_pubkeys") List<String> sshPublicKeys,
    @JsonProperty("created_by") String createdBy,
    @JsonProperty("bandwidth_ingress_bytes") long bandwidthIngressBytes,
    @JsonProperty("paused_at") Instant pausedAt,
    @JsonProperty("last_resumed_at") Instant lastResumedAt,
    @JsonProperty("forked_from") String forkedFrom,
    @JsonProperty("auto_pause_after_seconds") Integer autoPauseAfterSeconds,
    Map<String, String> labels) {
  /** Defensively copies collection values. */
  public SandboxData {
    egressRules = egressRules == null ? List.of() : List.copyOf(egressRules);
    environmentVariables =
        environmentVariables == null ? List.of() : List.copyOf(environmentVariables);
    sshPublicKeys = sshPublicKeys == null ? List.of() : List.copyOf(sshPublicKeys);
    labels = labels == null ? Map.of() : Map.copyOf(labels);
  }

  /** Creates a projection without labels, preserving the pre-labels constructor. */
  public SandboxData(
      String id,
      SandboxStatus status,
      String ipAddress,
      int virtualCpu,
      int memoryMib,
      long diskMib,
      Instant createdAt,
      boolean ingressEnabled,
      String ingressUrlTemplate,
      String name,
      Instant runningAt,
      Instant destroyedAt,
      double spawnMilliseconds,
      String shape,
      String rootFileSystem,
      String region,
      List<String> egressRules,
      List<String> environmentVariables,
      List<String> sshPublicKeys,
      String createdBy,
      long bandwidthIngressBytes,
      Instant pausedAt,
      Instant lastResumedAt,
      String forkedFrom,
      Integer autoPauseAfterSeconds) {
    this(
        id,
        status,
        ipAddress,
        virtualCpu,
        memoryMib,
        diskMib,
        createdAt,
        ingressEnabled,
        ingressUrlTemplate,
        name,
        runningAt,
        destroyedAt,
        spawnMilliseconds,
        shape,
        rootFileSystem,
        region,
        egressRules,
        environmentVariables,
        sshPublicKeys,
        createdBy,
        bandwidthIngressBytes,
        pausedAt,
        lastResumedAt,
        forkedFrom,
        autoPauseAfterSeconds,
        Map.of());
  }
}
