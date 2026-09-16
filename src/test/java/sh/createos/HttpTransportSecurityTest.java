package sh.createos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.createos.internal.HttpTransport;
import sh.createos.internal.HttpTransport.RetryPolicy;
import sh.createos.model.HealthResponse;
import sh.createos.model.Shape;

/** Security-focused transport contract tests. */
final class HttpTransportSecurityTest {
  private HttpServer server;
  private URI baseUri;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.start();
    baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void sdkAuthenticationCannotBeOverriddenByRequestHeaders() {
    AtomicReference<Headers> received = new AtomicReference<>();
    server.createContext(
        "/probe",
        exchange -> {
          received.set(exchange.getRequestHeaders());
          respond(exchange, 200, "{\"status\":\"success\",\"data\":{\"up\":true}}");
        });
    RequestOptions options =
        new RequestOptions(
            Map.of(
                "Authorization",
                "Bearer attacker",
                "X-Api-Key",
                "attacker",
                "Cookie",
                "session=secret",
                "X-Auth-Token",
                "secret",
                "X-CSRF-Token",
                "secret",
                "Proxy-Authorization",
                "secret",
                "X-Custom",
                "allowed"),
            null,
            null,
            true);

    transport("sdk-secret")
        .send("GET", "/probe", Map.of(), null, options, false, HealthResponse.class);

    assertEquals("sdk-secret", received.get().getFirst("X-Api-Key"));
    assertEquals("allowed", received.get().getFirst("X-Custom"));
    assertNull(received.get().getFirst("Authorization"));
    assertNull(received.get().getFirst("Cookie"));
    assertNull(received.get().getFirst("X-Auth-Token"));
    assertNull(received.get().getFirst("X-CSRF-Token"));
    assertNull(received.get().getFirst("Proxy-Authorization"));
  }

  @Test
  void unauthenticatedRequestsDropAllCallerCredentials() {
    AtomicReference<Headers> received = new AtomicReference<>();
    server.createContext(
        "/public",
        exchange -> {
          received.set(exchange.getRequestHeaders());
          respond(exchange, 200, "{\"status\":\"success\",\"data\":{\"up\":true}}");
        });
    RequestOptions options =
        new RequestOptions(
            Map.of("Authorization", "Bearer secret", "Cookie", "session=secret"), null, null, true);

    transport("sdk-secret")
        .send("GET", "/public", Map.of(), null, options, true, HealthResponse.class);

    assertNull(received.get().getFirst("X-Api-Key"));
    assertNull(received.get().getFirst("Authorization"));
    assertNull(received.get().getFirst("Cookie"));
  }

  @Test
  void protectedRequestsRequireAnApiKeyBeforeDispatch() {
    assertThrows(
        IllegalStateException.class,
        () ->
            transport(null)
                .send(
                    "GET",
                    "/never-reached",
                    Map.of(),
                    null,
                    RequestOptions.DEFAULT,
                    false,
                    HealthResponse.class));
  }

  @Test
  void transportRejectsRedirectFollowingHttpClients() {
    HttpClient redirectingClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new HttpTransport(
                baseUri,
                "sdk-secret",
                redirectingClient,
                "security-test",
                Duration.ofSeconds(5),
                new RetryPolicy(0, Duration.ofMillis(1), Duration.ofMillis(1))));
  }

  @Test
  void transportRejectsCredentialsAndQueriesInBaseUri() {
    assertThrows(
        IllegalArgumentException.class,
        () -> transportAt(URI.create("https://user:password@example.test")));
    assertThrows(
        IllegalArgumentException.class,
        () -> transportAt(URI.create("https://example.test?token=secret")));
    assertThrows(
        IllegalArgumentException.class, () -> transportAt(URI.create("file:///tmp/createos.sock")));
  }

  @Test
  void pathSegmentsCannotEscapeTheirResourceScope() {
    assertEquals("tenant%2Fother", HttpTransport.encodePathSegment("tenant/other"));
    assertEquals("%2E%2E", HttpTransport.encodePathSegment(".."));
    assertEquals("caf%C3%A9", HttpTransport.encodePathSegment("café"));
  }

  @Test
  void collectionEndpointsRejectFailureEnvelopesEvenWithHttpSuccess() {
    server.createContext(
        "/shapes",
        exchange ->
            respond(
                exchange,
                200,
                "{\"status\":\"fail\",\"message\":\"not authorized\",\"code\":40301}"));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                transport("sdk-secret")
                    .sendList(
                        "GET", "/shapes", Map.of(), RequestOptions.DEFAULT, false, Shape.class));

    assertEquals(40301, exception.serviceCode());
    assertEquals("not authorized", exception.getMessage());
  }

  @Test
  void emptySuccessResponseRequiresEnvelope() {
    server.createContext("/empty", exchange -> respond(exchange, 200, ""));

    assertThrows(
        IllegalStateException.class,
        () ->
            transport("sdk-secret")
                .send("GET", "/empty", Map.of(), null, RequestOptions.DEFAULT, false, Void.class));
  }

  private HttpTransport transport(String apiKey) {
    return transportAt(baseUri, apiKey);
  }

  private HttpTransport transportAt(URI uri) {
    return transportAt(uri, "sdk-secret");
  }

  private HttpTransport transportAt(URI uri, String apiKey) {
    return new HttpTransport(
        uri,
        apiKey,
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
        "security-test",
        Duration.ofSeconds(5),
        new RetryPolicy(0, Duration.ofMillis(1), Duration.ofMillis(1)));
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
