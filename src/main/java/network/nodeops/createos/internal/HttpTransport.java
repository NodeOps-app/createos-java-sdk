package network.nodeops.createos.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import network.nodeops.createos.ApiException;
import network.nodeops.createos.RequestOptions;

/** Internal synchronous HTTP transport. */
public final class HttpTransport {
  private static final int MAX_ERROR_BYTES = 4 * 1024 * 1024;
  private static final List<Integer> RETRYABLE_STATUSES = List.of(408, 429, 500, 502, 503, 504);

  private final URI baseUri;
  private final String baseOrigin;
  private final String apiKey;
  private final HttpClient httpClient;
  private final String userAgent;
  private final Duration timeout;
  private final RetryPolicy retryPolicy;
  private final ObjectMapper objectMapper;

  /** Creates a validated transport. */
  public HttpTransport(
      URI baseUri,
      String apiKey,
      HttpClient httpClient,
      String userAgent,
      Duration timeout,
      RetryPolicy retryPolicy) {
    this.baseUri = normalizeBaseUri(baseUri);
    this.baseOrigin = origin(this.baseUri);
    this.apiKey = apiKey;
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    if (httpClient.followRedirects() != HttpClient.Redirect.NEVER) {
      throw new IllegalArgumentException(
          "httpClient must disable redirects so API credentials cannot leave the configured "
              + "origin");
    }
    this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
    this.timeout = positive(timeout, "timeout");
    this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
    this.objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
  }

  /** Returns the configured wire object mapper. */
  public ObjectMapper objectMapper() {
    return objectMapper;
  }

  /** Sends a JSON request and unwraps its JSend response. */
  public <T> T send(
      String method,
      String path,
      Map<String, String> query,
      Object body,
      RequestOptions options,
      boolean skipAuthentication,
      Class<T> responseType) {
    RawResponse response =
        sendRaw(method, path, query, bodyPublisher(body), options, skipAuthentication, true);
    try (InputStream input = response.body()) {
      byte[] bytes = readResponseBody(response.statusCode(), input);
      requireSuccess(method, path, response, bytes);
      if (bytes.length == 0) {
        return null;
      }
      JsendEnvelope envelope = objectMapper.readValue(bytes, JsendEnvelope.class);
      if (!"success".equals(envelope.status())) {
        throw envelopeException(method, path, response, bytes, envelope);
      }
      if (responseType == Void.class) {
        return null;
      }
      if (envelope.data() == null || envelope.data().isNull()) {
        return null;
      }
      return objectMapper.treeToValue(envelope.data(), responseType);
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to read " + method + " " + path + " response", exception);
    }
  }

  /** Sends a request and decodes a collection from supported envelope shapes. */
  public <T> List<T> sendList(
      String method,
      String path,
      Map<String, String> query,
      RequestOptions options,
      boolean skipAuthentication,
      Class<T> itemType) {
    return sendPage(method, path, query, options, skipAuthentication, itemType).items();
  }

  /** Sends a request and decodes collection items with optional pagination metadata. */
  public <T> Page<T> sendPage(
      String method,
      String path,
      Map<String, String> query,
      RequestOptions options,
      boolean skipAuthentication,
      Class<T> itemType) {
    RawResponse response =
        sendRaw(
            method,
            path,
            query,
            HttpRequest.BodyPublishers.noBody(),
            options,
            skipAuthentication,
            true);
    try (InputStream input = response.body()) {
      byte[] bytes = readResponseBody(response.statusCode(), input);
      requireSuccess(method, path, response, bytes);
      JsendEnvelope envelope = objectMapper.readValue(bytes, JsendEnvelope.class);
      if (!"success".equals(envelope.status())) {
        throw envelopeException(method, path, response, bytes, envelope);
      }
      JsonNode data = envelope.data();
      Integer total = null;
      if (data != null && data.isObject()) {
        JsonNode pagination = data.get("pagination");
        if (pagination != null
            && pagination.has("total")
            && pagination.get("total").canConvertToInt()) {
          total = pagination.get("total").intValue();
        }
        if (data.has("data") && data.get("data").isArray()) {
          data = data.get("data");
        } else {
          java.util.Iterator<JsonNode> fields = data.elements();
          while (fields.hasNext()) {
            JsonNode candidate = fields.next();
            if (candidate.isArray()) {
              data = candidate;
              break;
            }
          }
        }
      }
      if (data == null || !data.isArray()) {
        return new Page<>(List.of(), total);
      }
      JavaType listType =
          objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
      return new Page<>(objectMapper.convertValue(data, listType), total);
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to read " + method + " " + path + " response", exception);
    }
  }

  /** Sends a request and transfers ownership of the response body to the caller. */
  public RawResponse sendRaw(
      String method,
      String path,
      Map<String, String> query,
      HttpRequest.BodyPublisher body,
      RequestOptions options,
      boolean skipAuthentication,
      boolean retryableBody) {
    RequestOptions effectiveOptions = options == null ? RequestOptions.DEFAULT : options;
    RetryPolicy effectiveRetry =
        effectiveOptions.retry() == null
            ? retryPolicy
            : new RetryPolicy(
                effectiveOptions.retry().maxRetries(),
                effectiveOptions.retry().baseDelay(),
                effectiveOptions.retry().maxDelay());
    int retries =
        effectiveOptions.disableRetry() || !retryableBody ? 0 : effectiveRetry.maxRetries();
    for (int attempt = 0; ; attempt++) {
      HttpRequest request =
          buildRequest(method, path, query, body, effectiveOptions, skipAuthentication);
      try {
        HttpResponse<InputStream> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (attempt >= retries || !canRetry(method, response.statusCode())) {
          return new RawResponse(response.statusCode(), response.headers(), response.body());
        }
        response.body().close();
        sleep(
            retryDelay(
                attempt,
                effectiveRetry,
                response.headers().firstValue("Retry-After").orElse(null)));
      } catch (IOException exception) {
        if (attempt >= retries || !isIdempotent(method)) {
          throw new IllegalStateException("HTTP request failed: " + method + " " + path, exception);
        }
        sleep(backoff(attempt, effectiveRetry));
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
            "HTTP request interrupted: " + method + " " + path, exception);
      }
    }
  }

  /** Throws an inspectable exception unless a raw response is successful. */
  public void requireSuccess(String method, String path, RawResponse response, byte[] body) {
    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      return;
    }
    throw apiException(method, path, response, body);
  }

  /** Reads at most the configured error-body inspection limit. */
  public byte[] readErrorBody(InputStream input) throws IOException {
    return input.readNBytes(MAX_ERROR_BYTES);
  }

  /** Percent-encodes one URL path segment. */
  public static String encodePathSegment(String value) {
    Objects.requireNonNull(value, "value");
    if (value.equals(".")) {
      return "%2E";
    }
    if (value.equals("..")) {
      return "%2E%2E";
    }
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    StringBuilder encoded = new StringBuilder(bytes.length);
    for (byte item : bytes) {
      int octet = item & 0xff;
      if (isUnreserved(octet)) {
        encoded.append((char) octet);
      } else {
        encoded.append('%');
        encoded.append(Character.toUpperCase(Character.forDigit(octet >>> 4, 16)));
        encoded.append(Character.toUpperCase(Character.forDigit(octet & 0x0f, 16)));
      }
    }
    return encoded.toString();
  }

  private HttpRequest buildRequest(
      String method,
      String path,
      Map<String, String> query,
      HttpRequest.BodyPublisher body,
      RequestOptions options,
      boolean skipAuthentication) {
    URI uri = resolve(path, query);
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(uri)
            .timeout(
                options.timeout() == null
                    ? timeout
                    : positive(options.timeout(), "request timeout"))
            .header("Accept", "application/json")
            .header("User-Agent", userAgent);
    for (Map.Entry<String, String> header : options.headers().entrySet()) {
      if (!isSensitiveHeader(header.getKey())) {
        builder.setHeader(header.getKey(), header.getValue());
      }
    }
    if (!skipAuthentication) {
      if (apiKey == null || apiKey.isBlank()) {
        throw new IllegalStateException("Authentication is required: configure an API key");
      }
      builder.setHeader("X-Api-Key", apiKey);
    }
    if (body.contentLength() != 0
        && options.headers().keySet().stream().noneMatch("content-type"::equalsIgnoreCase)) {
      builder.header("Content-Type", "application/json");
    }
    return builder.method(method, body).build();
  }

  private byte[] readResponseBody(int statusCode, InputStream input) throws IOException {
    return statusCode >= 200 && statusCode < 300 ? input.readAllBytes() : readErrorBody(input);
  }

  private HttpRequest.BodyPublisher bodyPublisher(Object body) {
    if (body == null) {
      return HttpRequest.BodyPublishers.noBody();
    }
    try {
      return HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(body));
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Failed to encode request body", exception);
    }
  }

  private URI resolve(String path, Map<String, String> query) {
    String relative = path.startsWith("/") ? path.substring(1) : path;
    URI resolved = baseUri.resolve(relative);
    if (!origin(resolved).equals(baseOrigin)) {
      throw new IllegalArgumentException("Request must remain on the configured origin");
    }
    if (query == null || query.isEmpty()) {
      return resolved;
    }
    StringBuilder value = new StringBuilder(resolved.toString());
    value.append(resolved.getQuery() == null ? '?' : '&');
    boolean first = true;
    for (Map.Entry<String, String> entry : query.entrySet()) {
      if (!first) {
        value.append('&');
      }
      first = false;
      value.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
    }
    return URI.create(value.toString());
  }

  private ApiException apiException(
      String method, String path, RawResponse response, byte[] responseBody) {
    byte[] limited =
        responseBody.length <= MAX_ERROR_BYTES
            ? responseBody
            : java.util.Arrays.copyOf(responseBody, MAX_ERROR_BYTES);
    String body = new String(limited, StandardCharsets.UTF_8);
    int code = 0;
    String message = null;
    try {
      JsendEnvelope envelope = objectMapper.readValue(limited, JsendEnvelope.class);
      code = envelope.code();
      message = envelope.message();
      if ((message == null || message.isBlank()) && envelope.data() != null) {
        JsonNode dataMessage = envelope.data().get("message");
        if (dataMessage != null && dataMessage.isTextual()) {
          message = dataMessage.textValue();
        }
      }
    } catch (IOException ignored) {
      // A non-JSend error body remains inspectable through responseBody().
    }
    if (message == null || message.isBlank()) {
      message = "request failed";
    }
    return new ApiException(
        method + " " + path + ": HTTP " + response.statusCode() + ": " + message,
        response.statusCode(),
        code,
        response.headers().firstValue("X-Request-ID").orElse(""),
        method,
        path,
        body,
        response.headers().map());
  }

  private ApiException envelopeException(
      String method, String path, RawResponse response, byte[] bytes, JsendEnvelope envelope) {
    return new ApiException(
        envelope.message() == null
            ? "Unexpected JSend status " + envelope.status()
            : envelope.message(),
        response.statusCode(),
        envelope.code(),
        response.headers().firstValue("X-Request-ID").orElse(""),
        method,
        path,
        new String(bytes, StandardCharsets.UTF_8),
        response.headers().map());
  }

  private static URI normalizeBaseUri(URI uri) {
    Objects.requireNonNull(uri, "baseUri");
    if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
        || uri.getHost() == null) {
      throw new IllegalArgumentException("base URI must be an absolute HTTP or HTTPS URI");
    }
    if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
      throw new IllegalArgumentException("base URI must not include user info, query, or fragment");
    }
    String value = uri.toString();
    return URI.create(value.endsWith("/") ? value : value + "/");
  }

  private static String origin(URI uri) {
    int port = uri.getPort();
    return uri.getScheme().toLowerCase(Locale.ROOT)
        + "://"
        + uri.getHost().toLowerCase(Locale.ROOT)
        + (port < 0 ? "" : ":" + port);
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private static boolean isSensitiveHeader(String name) {
    return List.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-auth-token",
            "x-csrf-token")
        .contains(name.toLowerCase(Locale.ROOT));
  }

  private static boolean isUnreserved(int octet) {
    return (octet >= 'a' && octet <= 'z')
        || (octet >= 'A' && octet <= 'Z')
        || (octet >= '0' && octet <= '9')
        || octet == '-'
        || octet == '.'
        || octet == '_'
        || octet == '~';
  }

  private static boolean isIdempotent(String method) {
    return List.of("GET", "HEAD", "OPTIONS", "PUT", "DELETE").contains(method);
  }

  private static boolean canRetry(String method, int status) {
    if (status == 429 || status == 503) {
      return true;
    }
    return isIdempotent(method) && RETRYABLE_STATUSES.contains(status);
  }

  private static Duration retryDelay(int attempt, RetryPolicy policy, String retryAfter) {
    if (retryAfter != null) {
      try {
        long seconds = Long.parseLong(retryAfter);
        return Duration.ofSeconds(Math.max(0, seconds));
      } catch (NumberFormatException ignored) {
        try {
          Duration delay =
              Duration.between(
                  Instant.now(),
                  ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME)
                      .toInstant());
          return delay.isNegative() ? Duration.ZERO : delay;
        } catch (DateTimeParseException ignoredAgain) {
          // Fall through to exponential backoff.
        }
      }
    }
    return backoff(attempt, policy);
  }

  private static Duration backoff(int attempt, RetryPolicy policy) {
    long exponential;
    try {
      exponential = Math.multiplyExact(policy.baseDelay().toMillis(), 1L << Math.min(attempt, 30));
    } catch (ArithmeticException exception) {
      exponential = policy.maxDelay().toMillis();
    }
    long jitter = ThreadLocalRandom.current().nextLong(Math.max(1, policy.baseDelay().toMillis()));
    return Duration.ofMillis(Math.min(exponential + jitter, policy.maxDelay().toMillis()));
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting to retry", exception);
    }
  }

  private static Duration positive(Duration value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return value;
  }

  /** Retry policy for idempotent requests. */
  public record RetryPolicy(int maxRetries, Duration baseDelay, Duration maxDelay) {
    /** Validates retry limits and delays. */
    public RetryPolicy {
      if (maxRetries < 0) {
        throw new IllegalArgumentException("maxRetries must not be negative");
      }
      positive(baseDelay, "baseDelay");
      if (maxDelay == null || maxDelay.compareTo(baseDelay) < 0) {
        throw new IllegalArgumentException("maxDelay must not be less than baseDelay");
      }
    }
  }

  /** Decoded collection page and optional total item count. */
  public record Page<T>(List<T> items, Integer total) {
    /** Defensively copies page items. */
    public Page {
      items = List.copyOf(items);
    }
  }
}
