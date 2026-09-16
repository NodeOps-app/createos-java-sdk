package sh.createos;

import java.util.List;
import java.util.Map;

/** Describes a non-successful response returned by the CreateOS API. */
public final class ApiException extends RuntimeException {
  private final int statusCode;
  private final int serviceCode;
  private final String requestId;
  private final String method;
  private final String endpoint;
  private final String responseBody;
  private final Map<String, List<String>> headers;

  /** Creates an API exception containing safe response metadata. */
  public ApiException(
      String message,
      int statusCode,
      int serviceCode,
      String requestId,
      String method,
      String endpoint,
      String responseBody,
      Map<String, List<String>> headers) {
    super(message);
    this.statusCode = statusCode;
    this.serviceCode = serviceCode;
    this.requestId = requestId;
    this.method = method;
    this.endpoint = endpoint;
    this.responseBody = responseBody;
    this.headers = Map.copyOf(headers);
  }

  /** Returns the HTTP response status. */
  public int statusCode() {
    return statusCode;
  }

  /** Returns the service-specific error code, or zero when absent. */
  public int serviceCode() {
    return serviceCode;
  }

  /** Returns the server request identifier, or an empty string when absent. */
  public String requestId() {
    return requestId;
  }

  /** Returns the request method. */
  public String method() {
    return method;
  }

  /** Returns the relative API endpoint. */
  public String endpoint() {
    return endpoint;
  }

  /** Returns the bounded response body captured for inspection. */
  public String responseBody() {
    return responseBody;
  }

  /** Returns response headers captured with the failure. */
  public Map<String, List<String>> headers() {
    return headers;
  }
}
