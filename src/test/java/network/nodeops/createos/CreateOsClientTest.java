package network.nodeops.createos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import network.nodeops.createos.model.CreateSandboxRequest;
import network.nodeops.createos.model.HealthResponse;
import network.nodeops.createos.model.Shape;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateOsClientTest {
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
  void healthDoesNotSendAuthentication() {
    AtomicReference<String> apiKey = new AtomicReference<>();
    server.createContext(
        "/healthz",
        exchange -> {
          apiKey.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
          respond(exchange, 200, "{\"status\":\"success\",\"data\":{\"up\":true}}");
        });

    HealthResponse response = client().health();

    assertTrue(response.up());
    assertEquals(null, apiKey.get());
  }

  @Test
  void createsSandboxWithExpectedWireNames() {
    AtomicReference<String> requestBody = new AtomicReference<>();
    AtomicReference<String> apiKey = new AtomicReference<>();
    server.createContext(
        "/v1/sandboxes",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          apiKey.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
          respond(
              exchange,
              200,
              """
              {"status":"success","data":{"id":"sb-1","status":"running","ip":"10.0.0.2","shape":"s-4vcpu-4gb","rootfs":"devbox:1","vcpu":4,"mem_mib":4096,"disk_mib":8192,"spawn_ms":42.5,"egress":[],"ingress_url_template":"https://<port>.example.test"}}
              """);
        });

    Sandbox sandbox =
        client()
            .createSandbox(
                CreateSandboxRequest.builder("s-4vcpu-4gb")
                    .rootFileSystem("devbox:1")
                    .ingressEnabled(true)
                    .build());

    assertEquals("sb-1", sandbox.id());
    assertEquals("secret", apiKey.get());
    assertTrue(requestBody.get().contains("\"rootfs\":\"devbox:1\""));
    assertTrue(requestBody.get().contains("\"ingress_enabled\":true"));
    assertFalse(requestBody.get().contains("disk_mib"));
  }

  @Test
  void decodesLegacyCollectionObjects() {
    server.createContext(
        "/v1/shapes",
        exchange ->
            respond(
                exchange,
                200,
                """
                {"status":"success","data":{"shapes":[{"id":"small","vcpu":2,
                "mem_mib":2048,"default_disk_mib":4096}]}}
                """));

    List<Shape> shapes = client().listShapes();

    assertEquals(1, shapes.size());
    assertEquals("small", shapes.get(0).id());
  }

  @Test
  void walksServerPaginationUsingTotalMetadata() {
    AtomicInteger requests = new AtomicInteger();
    server.createContext(
        "/v1/sandboxes",
        exchange -> {
          int offset = exchange.getRequestURI().getQuery().contains("offset=1") ? 1 : 0;
          requests.incrementAndGet();
          String status = offset == 0 ? "running" : "paused";
          respond(
              exchange,
              200,
              "{\"status\":\"success\",\"data\":{\"data\":[{\"id\":\"sb-"
                  + (offset + 1)
                  + "\",\"status\":\""
                  + status
                  + "\"}],\"pagination\":{\"total\":2,\"limit\":1,\"offset\":"
                  + offset
                  + ",\"count\":1}}}");
        });

    List<Sandbox> sandboxes =
        client().listSandboxes(network.nodeops.createos.model.ListSandboxesOptions.ALL);

    assertEquals(List.of("sb-1", "sb-2"), sandboxes.stream().map(Sandbox::id).toList());
    assertEquals(2, requests.get());
  }

  @Test
  void exposesApiErrorMetadata() {
    server.createContext(
        "/v1/whoami",
        exchange -> {
          exchange.getResponseHeaders().set("X-Request-ID", "req-123");
          respond(
              exchange, 401, "{\"status\":\"error\",\"message\":\"invalid key\",\"code\":1001}");
        });

    ApiException exception = assertThrows(ApiException.class, () -> client().whoAmI());

    assertEquals(401, exception.statusCode());
    assertEquals(1001, exception.serviceCode());
    assertEquals("req-123", exception.requestId());
  }

  private CreateOsClient client() {
    return CreateOsClient.builder().baseUri(baseUri).apiKey("secret").withoutRetry().build();
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
