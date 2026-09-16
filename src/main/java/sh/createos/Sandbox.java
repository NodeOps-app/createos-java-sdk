package sh.createos;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import sh.createos.internal.HttpTransport;
import sh.createos.internal.NdjsonStream;
import sh.createos.internal.RawResponse;
import sh.createos.model.Bandwidth;
import sh.createos.model.CommandStreamFrame;
import sh.createos.model.DiskAttachment;
import sh.createos.model.Egress;
import sh.createos.model.ForkSandboxRequest;
import sh.createos.model.PaginationOptions;
import sh.createos.model.ResizeSandboxResponse;
import sh.createos.model.RunCommandRequest;
import sh.createos.model.RunCommandResponse;
import sh.createos.model.SandboxData;
import sh.createos.model.SandboxDisk;
import sh.createos.model.SandboxStatus;

/** Stateful handle to one CreateOS sandbox. */
public final class Sandbox {
  private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofMinutes(2);
  private final HttpTransport transport;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final FilesService files;
  private final ProcessesService processes;
  private final ComputerService computer;
  private SandboxData data;

  Sandbox(HttpTransport transport, SandboxData data) {
    this.transport = transport;
    this.data = Objects.requireNonNull(data, "data");
    this.files = new FilesService(this);
    this.processes = new ProcessesService(this);
    this.computer = new ComputerService(this);
  }

  /** Returns the sandbox identifier. */
  public String id() {
    return data().id();
  }

  /** Returns the user-visible sandbox name. */
  public String name() {
    return data().name();
  }

  /** Returns the last observed lifecycle status. */
  public SandboxStatus status() {
    return data().status();
  }

  /** Returns the last observed private IP address. */
  public String ipAddress() {
    return data().ipAddress();
  }

  /** Returns the latest immutable server projection. */
  public SandboxData data() {
    lock.readLock().lock();
    try {
      return data;
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Returns file-transfer operations for this sandbox. */
  public FilesService files() {
    return files;
  }

  /** Returns persistent process and PTY operations for this sandbox. */
  public ProcessesService processes() {
    return processes;
  }

  /** Returns desktop computer-use operations for this sandbox. */
  public ComputerService computer() {
    return computer;
  }

  /** Refreshes this handle from the control plane. */
  public void refresh() {
    update(
        transport.send(
            "GET", path(""), Map.of(), null, RequestOptions.DEFAULT, false, SandboxData.class));
  }

  /** Pauses this sandbox and refreshes its cached projection. */
  public void pause() {
    lifecycle("/pause");
  }

  /** Resumes this sandbox and refreshes its cached projection. */
  public void resume() {
    lifecycle("/resume");
  }

  /** Creates an independent sandbox fork. */
  public Sandbox fork(ForkSandboxRequest request) {
    SandboxData forked =
        transport.send(
            "POST",
            path("/fork"),
            Map.of(),
            request,
            RequestOptions.DEFAULT,
            false,
            SandboxData.class);
    return new Sandbox(transport, forked);
  }

  /** Starts destruction of this sandbox. */
  public void destroy() {
    DestroyedResponse response =
        transport.send(
            "DELETE",
            path(""),
            Map.of(),
            null,
            RequestOptions.DEFAULT,
            false,
            DestroyedResponse.class);
    SandboxData current = data();
    update(copyWithStatus(current, response.status()));
  }

  /** Grows the sandbox overlay disk. */
  public ResizeSandboxResponse resize(long diskMib) {
    ResizeSandboxResponse response =
        transport.send(
            "POST",
            path("/resize"),
            Map.of(),
            Map.of("disk_mib", diskMib),
            RequestOptions.DEFAULT,
            false,
            ResizeSandboxResponse.class);
    refresh();
    return response;
  }

  /** Enables or disables public ingress. */
  public void setIngress(boolean enabled) {
    update(
        transport.send(
            "PATCH",
            path(""),
            Map.of(),
            Map.of("ingress_enabled", enabled),
            RequestOptions.DEFAULT,
            false,
            SandboxData.class));
  }

  /** Sets the idle auto-pause timeout, or disables it when {@code timeout} is null. */
  public void setAutoPause(Duration timeout) {
    Map<String, Object> request;
    if (timeout == null) {
      request = Map.of("disable_auto_pause", true);
    } else {
      long seconds = timeout.toSeconds();
      if (seconds < 60 || seconds > 24 * 60 * 60 || !timeout.equals(Duration.ofSeconds(seconds))) {
        throw new IllegalArgumentException(
            "auto-pause timeout must be whole seconds from 1 minute to 24 hours");
      }
      request = Map.of("auto_pause_after_seconds", seconds);
    }
    update(
        transport.send(
            "PATCH",
            path(""),
            Map.of(),
            request,
            RequestOptions.DEFAULT,
            false,
            SandboxData.class));
  }

  /** Adds OpenSSH public keys and returns the resulting key count. */
  public int addSshPublicKeys(List<String> keys) {
    AddKeysResponse response =
        transport.send(
            "POST",
            path("/ssh-pubkeys"),
            Map.of(),
            Map.of("keys", keys),
            RequestOptions.DEFAULT,
            false,
            AddKeysResponse.class);
    return response.count();
  }

  /** Waits until this sandbox reaches the running state. */
  public void waitUntilRunning(Duration timeout) {
    waitFor(SandboxStatus.RUNNING, timeout);
  }

  /** Waits until this sandbox reaches the paused state. */
  public void waitUntilPaused(Duration timeout) {
    waitFor(SandboxStatus.PAUSED, timeout);
  }

  /** Waits until this sandbox reaches the destroyed state. */
  public void waitUntilDestroyed(Duration timeout) {
    waitFor(SandboxStatus.DESTROYED, timeout);
  }

  /** Executes a command and buffers its output. */
  public RunCommandResponse runCommand(RunCommandRequest request) {
    return runCommand(request, RequestOptions.DEFAULT);
  }

  /** Executes a command with per-request transport overrides. */
  public RunCommandResponse runCommand(RunCommandRequest request, RequestOptions options) {
    RunCommandRequest buffered =
        new RunCommandRequest(
            request.command(),
            request.arguments(),
            request.standardInput(),
            request.environmentVariables(),
            false);
    return transport.send(
        "POST", path("/exec"), Map.of(), buffered, options, false, RunCommandResponse.class);
  }

  /** Runs a Bash script and throws when it exits unsuccessfully. */
  public RunCommandResponse shell(String script) {
    RunCommandResponse response = runCommand(RunCommandRequest.of("bash", "-lc", script));
    if (response.result().exitCode() != 0
        || (response.result().errorMessage() != null
            && !response.result().errorMessage().isEmpty())) {
      throw new CommandException(
          "command exited with status " + response.result().exitCode(), response);
    }
    return response;
  }

  /** Starts a streaming command. The returned stream must be closed. */
  public CommandStream streamCommand(RunCommandRequest request) {
    RawResponse response =
        transport.sendRaw(
            "POST",
            path("/exec"),
            Map.of("stream", "true"),
            jsonPublisher(request.streaming()),
            new RequestOptions(Map.of("Content-Type", "application/json"), null, null, true),
            false,
            false);
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      try (var body = response.body()) {
        transport.requireSuccess("POST", path("/exec"), response, transport.readErrorBody(body));
      } catch (java.io.IOException exception) {
        throw new IllegalStateException("Failed to read command stream error", exception);
      }
    }
    return new CommandStream(
        new NdjsonStream<>(response.body(), transport.objectMapper(), CommandStreamFrame.class));
  }

  /** Returns the current egress allowlist. */
  public Egress egress() {
    return transport.send(
        "GET", path("/egress"), Map.of(), null, RequestOptions.DEFAULT, false, Egress.class);
  }

  /** Replaces the egress allowlist. An empty list allows all egress. */
  public Egress setEgress(List<String> rules) {
    return transport.send(
        "PUT",
        path("/egress"),
        Map.of(),
        Map.of("egress", rules),
        RequestOptions.DEFAULT,
        false,
        Egress.class);
  }

  /** Returns bandwidth quota and usage counters. */
  public Bandwidth bandwidth() {
    return transport.send(
        "GET", path("/bandwidth"), Map.of(), null, RequestOptions.DEFAULT, false, Bandwidth.class);
  }

  /** Adds bytes to the sandbox bandwidth quota. */
  public Bandwidth rechargeBandwidth(long bytes) {
    return transport.send(
        "POST",
        path("/bandwidth/recharge"),
        Map.of(),
        Map.of("add_bytes", bytes),
        RequestOptions.DEFAULT,
        false,
        Bandwidth.class);
  }

  /** Attaches this sandbox to an overlay network. */
  public void attachNetwork(String networkId) {
    transport.send(
        "POST",
        path("/networks"),
        Map.of(),
        Map.of("id", networkId),
        RequestOptions.DEFAULT,
        false,
        Void.class);
  }

  /** Detaches this sandbox from an overlay network. */
  public void detachNetwork(String networkId) {
    transport.send(
        "DELETE",
        path("/networks/" + HttpTransport.encodePathSegment(networkId)),
        Map.of(),
        null,
        RequestOptions.DEFAULT,
        false,
        Void.class);
  }

  /** Lists persistent disks attached to this sandbox. */
  public List<SandboxDisk> listDisks(PaginationOptions options) {
    List<SandboxDisk> disks = new java.util.ArrayList<>();
    int offset = options.offset();
    while (options.limit() == 0 || disks.size() < options.limit()) {
      int pageSize = options.limit() == 0 ? 500 : Math.min(500, options.limit() - disks.size());
      Map<String, String> query =
          Map.of("limit", Integer.toString(pageSize), "offset", Integer.toString(offset));
      HttpTransport.Page<SandboxDisk> page =
          transport.sendPage(
              "GET", path("/disks"), query, RequestOptions.DEFAULT, false, SandboxDisk.class);
      disks.addAll(page.items());
      if (page.items().isEmpty()
          || page.total() == null
          || offset + page.items().size() >= page.total()) {
        break;
      }
      offset += page.items().size();
    }
    return List.copyOf(disks);
  }

  /** Mounts a registered persistent disk. */
  public void attachDisk(DiskAttachment attachment) {
    IdResponse response =
        transport.send(
            "POST",
            path("/disks"),
            Map.of(),
            attachment,
            RequestOptions.DEFAULT,
            false,
            IdResponse.class);
    if (!id().equals(response.id())) {
      throw new IllegalStateException(
          "server acknowledged an unexpected sandbox: " + response.id());
    }
  }

  /** Unmounts a persistent disk attachment. */
  public boolean detachDisk(String diskId, String mountPath) {
    Detached response =
        transport.send(
            "DELETE",
            path("/disks/" + HttpTransport.encodePathSegment(diskId)),
            Map.of("mount_path", mountPath),
            null,
            RequestOptions.DEFAULT,
            false,
            Detached.class);
    return response.detached();
  }

  /** Returns the public ingress URL for a sandbox port. */
  public URI previewUrl(int port) {
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("port must be between 1 and 65535");
    }
    SandboxData snapshot = data();
    if (!snapshot.ingressEnabled()
        || snapshot.ingressUrlTemplate() == null
        || snapshot.ingressUrlTemplate().isEmpty()) {
      throw new IllegalStateException("sandbox ingress is not enabled");
    }
    return URI.create(snapshot.ingressUrlTemplate().replace("<port>", Integer.toString(port)));
  }

  /** Waits until a TCP port is listening inside this sandbox. */
  public void waitForPort(String host, int port, Duration timeout) {
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("port must be between 1 and 65535");
    }
    String effectiveHost = host == null || host.isBlank() ? "127.0.0.1" : host;
    if (!effectiveHost.matches("[A-Za-z0-9.-]+")
        || java.util.Arrays.stream(effectiveHost.split("\\.", -1)).anyMatch(String::isEmpty)) {
      throw new IllegalArgumentException("host is not a valid IP address or DNS name");
    }
    Duration effectiveTimeout =
        timeout == null || timeout.isZero() || timeout.isNegative()
            ? Duration.ofSeconds(30)
            : timeout;
    long seconds = Math.max(1, (effectiveTimeout.toMillis() + 999) / 1000);
    String script =
        "timeout "
            + seconds
            + " bash -c 'until (echo > /dev/tcp/"
            + effectiveHost
            + "/"
            + port
            + ") 2>/dev/null; do sleep 0.25; done'";
    RunCommandResponse response =
        runCommand(
            RunCommandRequest.of("bash", "-c", script),
            new RequestOptions(
                Map.of(),
                effectiveTimeout.plusSeconds(5),
                null,
                RequestOptions.DEFAULT.disableRetry()));
    if (response.result().exitCode() != 0) {
      throw new IllegalStateException(
          "port " + port + " did not become ready within " + effectiveTimeout);
    }
  }

  HttpTransport transport() {
    return transport;
  }

  String path(String suffix) {
    return "/v1/sandboxes/" + HttpTransport.encodePathSegment(id()) + suffix;
  }

  private void lifecycle(String suffix) {
    update(
        transport.send(
            "POST",
            path(suffix),
            Map.of(),
            null,
            RequestOptions.DEFAULT,
            false,
            SandboxData.class));
  }

  private void waitFor(SandboxStatus expected, Duration requestedTimeout) {
    Duration timeout = requestedTimeout == null ? DEFAULT_WAIT_TIMEOUT : requestedTimeout;
    long deadline = System.nanoTime() + timeout.toNanos();
    while (true) {
      refresh();
      SandboxStatus status = status();
      if (status == expected) {
        return;
      }
      if (status == SandboxStatus.ERROR || status == SandboxStatus.FAILED) {
        throw new IllegalStateException("sandbox entered terminal state " + status);
      }
      if (System.nanoTime() >= deadline) {
        throw new IllegalStateException("timed out waiting for sandbox state " + expected);
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("interrupted while waiting for sandbox state", exception);
      }
    }
  }

  private HttpRequest.BodyPublisher jsonPublisher(Object value) {
    try {
      return HttpRequest.BodyPublishers.ofByteArray(
          transport.objectMapper().writeValueAsBytes(value));
    } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
      throw new IllegalArgumentException("Failed to encode request", exception);
    }
  }

  private void update(SandboxData newData) {
    lock.writeLock().lock();
    try {
      data = Objects.requireNonNull(newData, "newData");
    } finally {
      lock.writeLock().unlock();
    }
  }

  private static SandboxData copyWithStatus(SandboxData source, SandboxStatus status) {
    return new SandboxData(
        source.id(),
        status,
        source.ipAddress(),
        source.virtualCpu(),
        source.memoryMib(),
        source.diskMib(),
        source.createdAt(),
        source.ingressEnabled(),
        source.ingressUrlTemplate(),
        source.name(),
        source.runningAt(),
        source.destroyedAt(),
        source.spawnMilliseconds(),
        source.shape(),
        source.rootFileSystem(),
        source.region(),
        source.egressRules(),
        source.environmentVariables(),
        source.sshPublicKeys(),
        source.createdBy(),
        source.bandwidthIngressBytes(),
        source.pausedAt(),
        source.lastResumedAt(),
        source.forkedFrom(),
        source.autoPauseAfterSeconds());
  }

  private record DestroyedResponse(String id, SandboxStatus status) {}

  private record AddKeysResponse(int count) {}

  private record IdResponse(String id) {}

  private record Detached(boolean detached) {}
}
