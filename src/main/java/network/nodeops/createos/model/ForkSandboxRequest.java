package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** Optional overrides applied while forking a sandbox. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ForkSandboxRequest(
    @JsonProperty("start_paused") Boolean startPaused,
    @JsonProperty("ssh_pubkeys") List<String> sshPublicKeys,
    @JsonProperty("egress") List<String> egressRules,
    @JsonProperty("ingress_enabled") Boolean ingressEnabled,
    @JsonProperty("envs") Map<String, String> environmentVariables) {
  /** No fork overrides. */
  public static final ForkSandboxRequest DEFAULT =
      new ForkSandboxRequest(null, null, null, null, null);
}
