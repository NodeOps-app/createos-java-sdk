package sh.createos;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Objects;
import sh.createos.internal.RawResponse;

/** File transfer operations for one sandbox. */
public final class FilesService {
  private final Sandbox sandbox;

  FilesService(Sandbox sandbox) {
    this.sandbox = sandbox;
  }

  /** Uploads bytes to an absolute path inside the sandbox. */
  public void upload(String path, InputStream data) {
    upload(path, data, RequestOptions.DEFAULT);
  }

  /** Uploads bytes with per-request transport overrides. */
  public void upload(String path, InputStream data, RequestOptions options) {
    Objects.requireNonNull(data, "data");
    RequestOptions uploadOptions =
        new RequestOptions(
            merge(options.headers(), Map.of("Content-Type", "application/octet-stream")),
            options.timeout(),
            options.retry(),
            true);
    RawResponse response =
        sandbox
            .transport()
            .sendRaw(
                "PUT",
                sandbox.path("/files"),
                Map.of("path", path),
                HttpRequest.BodyPublishers.ofInputStream(() -> data),
                uploadOptions,
                false,
                false);
    try (InputStream body = response.body()) {
      byte[] bytes =
          response.statusCode() >= 200 && response.statusCode() < 300
              ? body.readAllBytes()
              : sandbox.transport().readErrorBody(body);
      sandbox.transport().requireSuccess("PUT", sandbox.path("/files"), response, bytes);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Failed to finish file upload", exception);
    }
  }

  /** Opens a sandbox file for reading. The caller must close the returned stream. */
  public InputStream download(String path) {
    return download(path, RequestOptions.DEFAULT);
  }

  /** Opens a sandbox file with per-request transport overrides. */
  public InputStream download(String path, RequestOptions options) {
    RawResponse response =
        sandbox
            .transport()
            .sendRaw(
                "GET",
                sandbox.path("/files"),
                Map.of("path", path),
                HttpRequest.BodyPublishers.noBody(),
                options,
                false,
                false);
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      try (InputStream body = response.body()) {
        sandbox
            .transport()
            .requireSuccess(
                "GET", sandbox.path("/files"), response, sandbox.transport().readErrorBody(body));
      } catch (java.io.IOException exception) {
        throw new IllegalStateException("Failed to read file-download error", exception);
      }
    }
    return response.body();
  }

  private static Map<String, String> merge(Map<String, String> left, Map<String, String> right) {
    java.util.HashMap<String, String> result = new java.util.HashMap<>(left);
    result.putAll(right);
    return Map.copyOf(result);
  }
}
