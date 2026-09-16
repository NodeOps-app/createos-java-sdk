package network.nodeops.createos;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import network.nodeops.createos.internal.HttpTransport;
import network.nodeops.createos.internal.HttpTransport.RetryPolicy;
import network.nodeops.createos.internal.RawResponse;
import network.nodeops.createos.model.CreateSandboxRequest;
import network.nodeops.createos.model.CreateSandboxResponse;
import network.nodeops.createos.model.HealthResponse;
import network.nodeops.createos.model.Host;
import network.nodeops.createos.model.ListSandboxesOptions;
import network.nodeops.createos.model.ReadyResponse;
import network.nodeops.createos.model.RootFileSystemCatalog;
import network.nodeops.createos.model.SandboxData;
import network.nodeops.createos.model.Shape;
import network.nodeops.createos.model.WhoAmI;

/** Authenticated entry point to the CreateOS Sandbox API. */
public final class CreateOsClient {
  private static final int MAXIMUM_PAGE_SIZE = 500;
  private final HttpTransport transport;
  private final URI baseUri;
  private final TemplatesService templates;
  private final NetworksService networks;
  private final DisksService disks;

  private CreateOsClient(Builder builder) {
    this.baseUri = builder.baseUri;
    this.transport =
        new HttpTransport(
            builder.baseUri,
            builder.apiKey,
            builder.httpClient,
            builder.userAgent,
            builder.timeout,
            builder.retryPolicy);
    this.templates = new TemplatesService(this);
    this.networks = new NetworksService(this);
    this.disks = new DisksService(this);
  }

  /** Creates a builder initialized from CreateOS environment variables. */
  public static Builder builder() {
    return new Builder();
  }

  /** Returns the configured control-plane base URI. */
  public URI baseUri() {
    return baseUri;
  }

  /** Returns account-level template operations. */
  public TemplatesService templates() {
    return templates;
  }

  /** Returns account-level overlay network operations. */
  public NetworksService networks() {
    return networks;
  }

  /** Returns account-level persistent disk operations. */
  public DisksService disks() {
    return disks;
  }

  /** Returns the unauthenticated control-plane liveness state. */
  public HealthResponse health() {
    return transport.send(
        "GET", "/healthz", Map.of(), null, RequestOptions.DEFAULT, true, HealthResponse.class);
  }

  /** Returns the unauthenticated control-plane readiness state. */
  public ReadyResponse ready() {
    String path = "/readyz";
    RawResponse response =
        transport.sendRaw(
            "GET",
            path,
            Map.of(),
            HttpRequest.BodyPublishers.noBody(),
            new RequestOptions(Map.of(), null, null, true),
            true,
            true);
    try (var body = response.body()) {
      byte[] bytes = body.readAllBytes();
      if (response.statusCode() != 200 && response.statusCode() != 503) {
        transport.requireSuccess("GET", path, response, bytes);
      }
      if (bytes.length == 0) {
        return new ReadyResponse(response.statusCode() == 200, null, 0);
      }
      try {
        var envelope = transport.objectMapper().readTree(bytes);
        var data = envelope.get("data");
        return data == null || data.isNull()
            ? new ReadyResponse(response.statusCode() == 200, null, 0)
            : transport.objectMapper().treeToValue(data, ReadyResponse.class);
      } catch (java.io.IOException ignored) {
        return new ReadyResponse(response.statusCode() == 200, null, 0);
      }
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Failed to read readiness response", exception);
    }
  }

  /** Returns the identity associated with the configured API key. */
  public WhoAmI whoAmI() {
    return transport.send(
        "GET", "/v1/whoami", Map.of(), null, RequestOptions.DEFAULT, false, WhoAmI.class);
  }

  /** Creates a running sandbox and returns its connected handle. */
  public Sandbox createSandbox(CreateSandboxRequest request) {
    CreateSandboxResponse created =
        transport.send(
            "POST",
            "/v1/sandboxes",
            Map.of(),
            request,
            RequestOptions.DEFAULT,
            false,
            CreateSandboxResponse.class);
    SandboxData data =
        new SandboxData(
            created.id(),
            created.status(),
            created.ipAddress(),
            created.virtualCpu(),
            created.memoryMib(),
            created.diskMib(),
            null,
            Boolean.TRUE.equals(request.ingressEnabled()),
            created.ingressUrlTemplate(),
            created.name(),
            null,
            null,
            created.spawnMilliseconds(),
            created.shape(),
            created.rootFileSystem(),
            request.region(),
            created.egressRules(),
            List.of(),
            request.sshPublicKeys(),
            null,
            0,
            null,
            null,
            null,
            request.autoPauseAfterSeconds());
    return new Sandbox(transport, data);
  }

  /** Returns a connected handle for a sandbox ID. */
  public Sandbox getSandbox(String sandboxId) {
    return getSandboxAt("/v1/sandboxes/" + HttpTransport.encodePathSegment(sandboxId));
  }

  /** Returns a connected handle for a sandbox private IP address. */
  public Sandbox getSandboxByIp(String ipAddress) {
    return getSandboxAt("/v1/sandboxes/by-ip/" + HttpTransport.encodePathSegment(ipAddress));
  }

  /** Lists connected sandbox handles, walking server-side pages as needed. */
  public List<Sandbox> listSandboxes(ListSandboxesOptions options) {
    Map<String, String> query = new HashMap<>();
    if (options.status() != null) {
      query.put("status", options.status().wireValue());
    }
    return fetchAll("/v1/sandboxes", query, options.limit(), false, SandboxData.class).stream()
        .map(data -> new Sandbox(transport, data))
        .toList();
  }

  /** Lists available sizing presets. */
  public List<Shape> listShapes() {
    return fetchAll("/v1/shapes", Map.of(), 0, true, Shape.class);
  }

  /** Returns the built-in root filesystem catalog. */
  public RootFileSystemCatalog listRootFileSystems() {
    return transport.send(
        "GET",
        "/v1/rootfs",
        Map.of(),
        null,
        RequestOptions.DEFAULT,
        true,
        RootFileSystemCatalog.class);
  }

  /** Lists public worker hosts. Administrator credentials are required. */
  public List<Host> listHosts() {
    return fetchAll("/v1/hosts", Map.of(), 0, false, Host.class);
  }

  HttpTransport transport() {
    return transport;
  }

  <T> List<T> fetchAll(
      String path,
      Map<String, String> baseQuery,
      int resultLimit,
      boolean skipAuthentication,
      Class<T> itemType) {
    List<T> items = new ArrayList<>();
    int offset = Integer.parseInt(baseQuery.getOrDefault("offset", "0"));
    while (resultLimit == 0 || items.size() < resultLimit) {
      int pageSize =
          resultLimit == 0
              ? MAXIMUM_PAGE_SIZE
              : Math.min(MAXIMUM_PAGE_SIZE, resultLimit - items.size());
      Map<String, String> query = new HashMap<>(baseQuery);
      query.put("limit", Integer.toString(pageSize));
      query.put("offset", Integer.toString(offset));
      HttpTransport.Page<T> page =
          transport.sendPage(
              "GET", path, query, RequestOptions.DEFAULT, skipAuthentication, itemType);
      items.addAll(page.items());
      if (page.items().isEmpty()
          || page.total() == null
          || offset + page.items().size() >= page.total()) {
        break;
      }
      offset += page.items().size();
    }
    return resultLimit > 0 && items.size() > resultLimit
        ? List.copyOf(items.subList(0, resultLimit))
        : List.copyOf(items);
  }

  private Sandbox getSandboxAt(String path) {
    SandboxData data =
        transport.send(
            "GET", path, Map.of(), null, RequestOptions.DEFAULT, false, SandboxData.class);
    return new Sandbox(transport, data);
  }

  /** Builder for a CreateOS client. */
  public static final class Builder {
    private String apiKey = trimToNull(System.getenv("CREATEOS_SANDBOX_API_KEY"));
    private URI baseUri = baseUriFromEnvironment();
    private HttpClient httpClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private String userAgent = "createos-java-sdk/0.1.0";
    private Duration timeout = Duration.ofSeconds(60);
    private RetryPolicy retryPolicy =
        new RetryPolicy(2, Duration.ofMillis(500), Duration.ofSeconds(30));

    private Builder() {}

    /** Configures the API key. */
    public Builder apiKey(String apiKey) {
      this.apiKey = requireNonBlank(apiKey, "apiKey");
      return this;
    }

    /** Configures the control-plane base URI. */
    public Builder baseUri(URI baseUri) {
      this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
      return this;
    }

    /** Configures an HTTP client that rejects redirects. */
    public Builder httpClient(HttpClient httpClient) {
      this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
      return this;
    }

    /** Configures the User-Agent header. */
    public Builder userAgent(String userAgent) {
      this.userAgent = requireNonBlank(userAgent, "userAgent");
      return this;
    }

    /** Configures the default request timeout. */
    public Builder timeout(Duration timeout) {
      this.timeout = Objects.requireNonNull(timeout, "timeout");
      return this;
    }

    /** Configures idempotent-request retry behavior. */
    public Builder retry(int maxRetries, Duration baseDelay, Duration maxDelay) {
      this.retryPolicy = new RetryPolicy(maxRetries, baseDelay, maxDelay);
      return this;
    }

    /** Disables automatic retries. */
    public Builder withoutRetry() {
      this.retryPolicy = new RetryPolicy(0, Duration.ofMillis(500), Duration.ofSeconds(30));
      return this;
    }

    /** Builds the configured client. */
    public CreateOsClient build() {
      return new CreateOsClient(this);
    }

    private static URI baseUriFromEnvironment() {
      String configured = System.getenv("CREATEOS_SANDBOX_BASE_URL");
      return URI.create(
          configured == null || configured.isBlank()
              ? "https://api.sb.createos.sh"
              : configured.trim());
    }

    private static String requireNonBlank(String value, String name) {
      Objects.requireNonNull(value, name);
      if (value.isBlank()) {
        throw new IllegalArgumentException(name + " must not be blank");
      }
      return value.trim();
    }

    private static String trimToNull(String value) {
      if (value == null || value.isBlank()) {
        return null;
      }
      return value.trim();
    }
  }
}
