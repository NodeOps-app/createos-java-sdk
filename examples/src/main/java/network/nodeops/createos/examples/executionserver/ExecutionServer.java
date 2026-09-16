package network.nodeops.createos.examples.executionserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import network.nodeops.createos.CreateOsClient;
import network.nodeops.createos.RequestOptions;
import network.nodeops.createos.Sandbox;
import network.nodeops.createos.model.CreateSandboxRequest;
import network.nodeops.createos.model.RunCommandRequest;

/** Local HTTP service that executes each request in a fresh sandbox. */
public final class ExecutionServer {
  private static final int MAXIMUM_REQUEST_BYTES = 1 << 20;
  private static final int MAXIMUM_CONCURRENCY = 4;
  private static final Duration EXECUTION_TIMEOUT = Duration.ofMinutes(2);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final CreateOsClient client;
  private final Semaphore slots = new Semaphore(MAXIMUM_CONCURRENCY);

  private ExecutionServer(CreateOsClient client) {
    this.client = client;
  }

  /** Starts the server and leaves it running until the process is interrupted. */
  public static void main(String[] arguments) throws IOException {
    String configured = System.getenv("EXECUTION_SERVER_ADDRESS");
    InetSocketAddress address = parseAddress(configured == null ? "127.0.0.1:8080" : configured);
    ExecutionServer application = new ExecutionServer(CreateOsClient.builder().build());
    HttpServer server = HttpServer.create(address, 0);
    server.createContext("/v1/execute", application::handle);
    server.setExecutor(Executors.newFixedThreadPool(MAXIMUM_CONCURRENCY));
    Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(10)));
    server.start();
    System.out.println("execution server listening on " + address);
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      try {
        if (!"POST".equals(exchange.getRequestMethod())) {
          exchange.getResponseHeaders().set("Allow", "POST");
          write(exchange, 405, new ErrorResponse("method not allowed"));
          return;
        }
        if (!slots.tryAcquire()) {
          write(exchange, 429, new ErrorResponse("execution capacity reached"));
          return;
        }
        try {
          execute(exchange);
        } finally {
          slots.release();
        }
      } catch (RuntimeException exception) {
        write(exchange, 502, new ErrorResponse("execution failed: " + exception.getMessage()));
      }
    }
  }

  private void execute(HttpExchange exchange) throws IOException {
    byte[] body = exchange.getRequestBody().readNBytes(MAXIMUM_REQUEST_BYTES + 1);
    if (body.length > MAXIMUM_REQUEST_BYTES) {
      write(exchange, 413, new ErrorResponse("request body exceeds 1 MiB"));
      return;
    }
    ExecuteRequest input;
    try {
      input = OBJECT_MAPPER.readValue(body, ExecuteRequest.class);
    } catch (IOException exception) {
      write(exchange, 400, new ErrorResponse("invalid JSON request"));
      return;
    }
    if (input.command() == null || input.command().isBlank()) {
      write(exchange, 400, new ErrorResponse("command is required"));
      return;
    }

    Sandbox sandbox =
        client.createSandbox(
            CreateSandboxRequest.builder("s-1vcpu-1gb").rootFileSystem("devbox:1").build());
    try {
      var request =
          new RunCommandRequest(
              input.command().trim(),
              input.arguments(),
              input.standardInput(),
              input.environmentVariables(),
              false);
      var result =
          sandbox.runCommand(request, new RequestOptions(Map.of(), EXECUTION_TIMEOUT, null, true));
      write(
          exchange,
          200,
          new ExecuteResponse(
              result.result().standardOutput(),
              result.result().standardError(),
              result.result().exitCode(),
              result.result().errorMessage(),
              result.executionMilliseconds()));
    } finally {
      sandbox.destroy();
    }
  }

  private static void write(HttpExchange exchange, int status, Object value) throws IOException {
    byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(value);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
  }

  private static InetSocketAddress parseAddress(String value) {
    int separator = value.lastIndexOf(':');
    if (separator < 1 || separator == value.length() - 1) {
      throw new IllegalArgumentException("address must use host:port format");
    }
    return new InetSocketAddress(
        value.substring(0, separator), Integer.parseInt(value.substring(separator + 1)));
  }

  @JsonIgnoreProperties(ignoreUnknown = false)
  private record ExecuteRequest(
      String command,
      List<String> arguments,
      String standardInput,
      Map<String, String> environmentVariables) {
    private ExecuteRequest {
      arguments = arguments == null ? List.of() : List.copyOf(arguments);
      environmentVariables =
          environmentVariables == null ? Map.of() : Map.copyOf(environmentVariables);
    }
  }

  private record ExecuteResponse(
      String stdout, String stderr, int exitCode, String error, double executionMilliseconds) {}

  private record ErrorResponse(String error) {}
}
