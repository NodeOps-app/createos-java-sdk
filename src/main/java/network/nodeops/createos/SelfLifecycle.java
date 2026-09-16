package network.nodeops.createos;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/** In-sandbox lifecycle signals sent to the local CreateOS agent. */
public final class SelfLifecycle {
  private static final URI AGENT_URI = URI.create("http://127.0.0.1:1029/self/");

  private SelfLifecycle() {}

  /** Requests that the current sandbox pause itself. */
  public static void pause(String reason) {
    signal("pause", reason);
  }

  /** Requests irreversible deletion of the current sandbox. */
  public static void delete(String reason) {
    signal("delete", reason);
  }

  private static void signal(String action, String reason) {
    String query =
        reason == null || reason.isBlank()
            ? ""
            : "?reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8).replace("+", "%20");
    HttpRequest request =
        HttpRequest.newBuilder(AGENT_URI.resolve(action + query))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
    try {
      HttpResponse<Void> response =
          HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() != 202) {
        throw new IllegalStateException(
            "self-" + action + " request returned HTTP " + response.statusCode());
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to send self-" + action + " request", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Self-" + action + " request was interrupted", exception);
    }
  }
}
