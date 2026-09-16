package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Request used to create a sandbox. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CreateSandboxRequest(
    String shape,
    @JsonProperty("rootfs") String rootFileSystem,
    String name,
    List<NetworkEntry> networks,
    @JsonProperty("disk_mib") Long diskMib,
    @JsonProperty("egress") List<String> egressRules,
    @JsonProperty("envs") Map<String, String> environmentVariables,
    @JsonProperty("ssh_pubkeys") List<String> sshPublicKeys,
    @JsonProperty("host_id") String hostId,
    @JsonProperty("node_selector") Map<String, String> nodeSelector,
    @JsonProperty("ingress_enabled") Boolean ingressEnabled,
    List<DiskAttachment> disks,
    String region,
    @JsonProperty("auto_pause_after_seconds") Integer autoPauseAfterSeconds) {
  /** Validates required values and defensively copies collection values. */
  public CreateSandboxRequest {
    shape = Objects.requireNonNull(shape, "shape");
    if (shape.isBlank()) {
      throw new IllegalArgumentException("shape must not be blank");
    }
    networks = networks == null ? List.of() : List.copyOf(networks);
    egressRules = egressRules == null ? List.of() : List.copyOf(egressRules);
    environmentVariables =
        environmentVariables == null ? Map.of() : Map.copyOf(environmentVariables);
    sshPublicKeys = sshPublicKeys == null ? List.of() : List.copyOf(sshPublicKeys);
    nodeSelector = nodeSelector == null ? Map.of() : Map.copyOf(nodeSelector);
    disks = disks == null ? List.of() : List.copyOf(disks);
  }

  /** Returns a builder requiring the sandbox shape. */
  public static Builder builder(String shape) {
    return new Builder(shape);
  }

  /** Builder for optional sandbox creation settings. */
  public static final class Builder {
    private final String shape;
    private String rootFileSystem;
    private String name;
    private List<NetworkEntry> networks = List.of();
    private Long diskMib;
    private List<String> egressRules = List.of();
    private Map<String, String> environmentVariables = Map.of();
    private List<String> sshPublicKeys = List.of();
    private String hostId;
    private Map<String, String> nodeSelector = Map.of();
    private Boolean ingressEnabled;
    private List<DiskAttachment> disks = List.of();
    private String region;
    private Integer autoPauseAfterSeconds;

    private Builder(String shape) {
      this.shape = shape;
    }

    /** Sets the built-in root filesystem name or custom template identifier. */
    public Builder rootFileSystem(String rootFileSystem) {
      this.rootFileSystem = rootFileSystem;
      return this;
    }

    /** Sets the human-readable sandbox name. */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /** Sets overlay networks attached during creation. */
    public Builder networks(List<NetworkEntry> networks) {
      this.networks = networks;
      return this;
    }

    /** Sets the overlay disk size in mebibytes. */
    public Builder diskMib(long diskMib) {
      this.diskMib = diskMib;
      return this;
    }

    /** Sets the outbound network allowlist. */
    public Builder egressRules(List<String> egressRules) {
      this.egressRules = egressRules;
      return this;
    }

    /** Sets environment variables injected into the sandbox. */
    public Builder environmentVariables(Map<String, String> environmentVariables) {
      this.environmentVariables = environmentVariables;
      return this;
    }

    /** Sets authorized OpenSSH public keys. */
    public Builder sshPublicKeys(List<String> sshPublicKeys) {
      this.sshPublicKeys = sshPublicKeys;
      return this;
    }

    /** Requests placement on a specific worker host. */
    public Builder hostId(String hostId) {
      this.hostId = hostId;
      return this;
    }

    /** Requests placement on a worker matching the supplied labels. */
    public Builder nodeSelector(Map<String, String> nodeSelector) {
      this.nodeSelector = nodeSelector;
      return this;
    }

    /** Enables or disables public preview ingress. */
    public Builder ingressEnabled(boolean ingressEnabled) {
      this.ingressEnabled = ingressEnabled;
      return this;
    }

    /** Sets persistent disks mounted during creation. */
    public Builder disks(List<DiskAttachment> disks) {
      this.disks = disks;
      return this;
    }

    /** Requests placement in a specific region. */
    public Builder region(String region) {
      this.region = region;
      return this;
    }

    /** Sets the idle auto-pause timeout in seconds. */
    public Builder autoPauseAfterSeconds(int autoPauseAfterSeconds) {
      this.autoPauseAfterSeconds = autoPauseAfterSeconds;
      return this;
    }

    /** Builds an immutable creation request. */
    public CreateSandboxRequest build() {
      return new CreateSandboxRequest(
          shape,
          rootFileSystem,
          name,
          networks,
          diskMib,
          egressRules,
          environmentVariables,
          sshPublicKeys,
          hostId,
          nodeSelector,
          ingressEnabled,
          disks,
          region,
          autoPauseAfterSeconds);
    }
  }
}
