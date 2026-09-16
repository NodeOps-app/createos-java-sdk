package sh.createos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sh.createos.internal.HttpTransport;
import sh.createos.internal.NdjsonStream;
import sh.createos.internal.RawResponse;
import sh.createos.model.CreateTemplateRequest;
import sh.createos.model.PaginationOptions;
import sh.createos.model.Template;
import sh.createos.model.TemplateLogEvent;

/** Account-level custom root filesystem template operations. */
public final class TemplatesService {
  private final CreateOsClient client;

  TemplatesService(CreateOsClient client) {
    this.client = client;
  }

  /** Lists templates owned by the caller. */
  public List<Template> list(PaginationOptions options) {
    return client.fetchAll(
        "/v1/templates",
        options.offset() == 0 ? Map.of() : Map.of("offset", Integer.toString(options.offset())),
        options.limit(),
        false,
        Template.class);
  }

  /** Submits a Dockerfile template build. */
  public Template create(CreateTemplateRequest request) {
    return client
        .transport()
        .send(
            "POST",
            "/v1/templates",
            Map.of(),
            request,
            RequestOptions.DEFAULT,
            false,
            Template.class);
  }

  /** Returns a template, optionally including its Dockerfile. */
  public Template get(String templateId, boolean includeDockerfile) {
    return client
        .transport()
        .send(
            "GET",
            path(templateId),
            includeDockerfile ? Map.of("include", "dockerfile") : Map.of(),
            null,
            RequestOptions.DEFAULT,
            false,
            Template.class);
  }

  /** Deletes a template without affecting existing sandboxes. */
  public void delete(String templateId) {
    client
        .transport()
        .send(
            "DELETE", path(templateId), Map.of(), null, RequestOptions.DEFAULT, false, Void.class);
  }

  /** Returns the build log collected so far as plain text. */
  public String logs(String templateId, int attempt) {
    Map<String, String> query =
        attempt > 0 ? Map.of("attempt", Integer.toString(attempt)) : Map.of();
    String path = path(templateId) + "/logs";
    RawResponse response =
        client
            .transport()
            .sendRaw(
                "GET",
                path,
                query,
                java.net.http.HttpRequest.BodyPublishers.noBody(),
                RequestOptions.DEFAULT,
                false,
                true);
    try (var body = response.body()) {
      byte[] bytes = body.readAllBytes();
      client.transport().requireSuccess("GET", path, response, bytes);
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read template logs", exception);
    }
  }

  /** Follows template build-log events. The returned stream must be closed. */
  public NdjsonStream<TemplateLogEvent> followLogs(String templateId, int attempt) {
    return followLogs(templateId, attempt, RequestOptions.DEFAULT);
  }

  /** Follows build-log events with per-request transport options. */
  public NdjsonStream<TemplateLogEvent> followLogs(
      String templateId, int attempt, RequestOptions options) {
    Map<String, String> query = new HashMap<>();
    query.put("follow", "true");
    if (attempt > 0) {
      query.put("attempt", Integer.toString(attempt));
    }
    String path = path(templateId) + "/logs";
    RawResponse response =
        client
            .transport()
            .sendRaw(
                "GET",
                path,
                query,
                java.net.http.HttpRequest.BodyPublishers.noBody(),
                options,
                false,
                false);
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      try (var body = response.body()) {
        client
            .transport()
            .requireSuccess("GET", path, response, client.transport().readErrorBody(body));
      } catch (IOException exception) {
        throw new IllegalStateException("Failed to read template stream error", exception);
      }
    }
    return new NdjsonStream<>(
        response.body(), client.transport().objectMapper(), TemplateLogEvent.class);
  }

  private static String path(String templateId) {
    return "/v1/templates/" + HttpTransport.encodePathSegment(templateId);
  }
}
