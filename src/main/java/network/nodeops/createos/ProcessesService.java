package network.nodeops.createos;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import network.nodeops.createos.internal.HttpTransport;
import network.nodeops.createos.internal.NdjsonStream;
import network.nodeops.createos.internal.RawResponse;
import network.nodeops.createos.model.CreateProcessRequest;
import network.nodeops.createos.model.ManagedProcess;
import network.nodeops.createos.model.ProcessStreamFrame;
import network.nodeops.createos.model.PtySize;

/** Persistent process and PTY operations for one sandbox. */
public final class ProcessesService {
  private final Sandbox sandbox;

  ProcessesService(Sandbox sandbox) {
    this.sandbox = sandbox;
  }

  /** Starts a managed process or PTY. */
  public ManagedProcess create(CreateProcessRequest request) {
    return sandbox
        .transport()
        .send(
            "POST",
            path("", ""),
            Map.of(),
            request,
            RequestOptions.DEFAULT,
            false,
            ManagedProcess.class);
  }

  /** Lists retained managed processes and PTYs. */
  public List<ManagedProcess> list() {
    ProcessList response =
        sandbox
            .transport()
            .send(
                "GET",
                path("", ""),
                Map.of(),
                null,
                RequestOptions.DEFAULT,
                false,
                ProcessList.class);
    return response.processes();
  }

  /** Returns a managed process by identifier. */
  public ManagedProcess get(String processId) {
    return sandbox
        .transport()
        .send(
            "GET",
            path(processId, ""),
            Map.of(),
            null,
            RequestOptions.DEFAULT,
            false,
            ManagedProcess.class);
  }

  /** Opens a replayable process output stream. */
  public ProcessStream connect(String processId, long afterSequence) {
    String path = path(processId, "/connect");
    RawResponse response =
        sandbox
            .transport()
            .sendRaw(
                "GET",
                path,
                Map.of("after", Long.toString(afterSequence)),
                HttpRequest.BodyPublishers.noBody(),
                new RequestOptions(Map.of(), null, null, true),
                false,
                false);
    requireStreamingSuccess("GET", path, response);
    return new ProcessStream(
        new NdjsonStream<>(
            response.body(), sandbox.transport().objectMapper(), ProcessStreamFrame.class));
  }

  /** Writes UTF-8 input to a managed process. */
  public long input(String processId, String data) {
    return input(processId, data.getBytes(StandardCharsets.UTF_8));
  }

  /** Writes binary input to a managed process. */
  public long input(String processId, byte[] data) {
    InputResponse response =
        sandbox
            .transport()
            .send(
                "POST",
                path(processId, "/input"),
                Map.of(),
                Map.of("data_base64", Base64.getEncoder().encodeToString(data)),
                RequestOptions.DEFAULT,
                false,
                InputResponse.class);
    return response.inputSequence();
  }

  /** Closes a pipe process's standard input. */
  public void closeStandardInput(String processId) {
    control(processId, "/stdin/close", null);
  }

  /** Changes PTY dimensions. */
  public void resize(String processId, PtySize size) {
    control(processId, "/resize", size);
  }

  /** Sends a signal name such as {@code SIGTERM}. */
  public void signal(String processId, String signal) {
    control(processId, "/signal", Map.of("signal", signal));
  }

  /** Long-polls until the process leader or complete process tree exits. */
  public ManagedProcess waitFor(String processId, String scope, Duration waitTimeout) {
    java.util.HashMap<String, String> query = new java.util.HashMap<>();
    if (scope != null && !scope.isBlank()) {
      query.put("scope", scope);
    }
    if (waitTimeout != null && !waitTimeout.isNegative() && !waitTimeout.isZero()) {
      query.put("timeout_ms", Long.toString(waitTimeout.toMillis()));
    }
    return sandbox
        .transport()
        .send(
            "GET",
            path(processId, "/wait"),
            query,
            null,
            RequestOptions.DEFAULT,
            false,
            ManagedProcess.class);
  }

  /** Terminates a managed process tree. */
  public ManagedProcess delete(String processId, Duration gracePeriod) {
    Map<String, String> query =
        gracePeriod == null ? Map.of() : Map.of("grace_ms", Long.toString(gracePeriod.toMillis()));
    return sandbox
        .transport()
        .send(
            "DELETE",
            path(processId, ""),
            query,
            null,
            RequestOptions.DEFAULT,
            false,
            ManagedProcess.class);
  }

  private void control(String processId, String suffix, Object body) {
    sandbox
        .transport()
        .send(
            "POST",
            path(processId, suffix),
            Map.of(),
            body,
            RequestOptions.DEFAULT,
            false,
            Void.class);
  }

  private String path(String processId, String suffix) {
    String value = sandbox.path("/processes");
    if (processId != null && !processId.isEmpty()) {
      value += "/" + HttpTransport.encodePathSegment(processId);
    }
    return value + suffix;
  }

  private void requireStreamingSuccess(String method, String path, RawResponse response) {
    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      return;
    }
    try (var body = response.body()) {
      sandbox
          .transport()
          .requireSuccess(method, path, response, sandbox.transport().readErrorBody(body));
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read process-stream error", exception);
    }
  }

  private record ProcessList(List<ManagedProcess> processes) {}

  private record InputResponse(
      @com.fasterxml.jackson.annotation.JsonProperty("input_seq") long inputSequence) {}
}
