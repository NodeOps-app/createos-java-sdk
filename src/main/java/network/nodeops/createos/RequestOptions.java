package network.nodeops.createos;

import java.time.Duration;
import java.util.Map;

/** Per-request transport overrides. */
public record RequestOptions(
    Map<String, String> headers, Duration timeout, RetryOptions retry, boolean disableRetry) {
  /** Default request options. */
  public static final RequestOptions DEFAULT = new RequestOptions(Map.of(), null, null, false);

  /** Defensively copies request headers. */
  public RequestOptions {
    headers = headers == null ? Map.of() : Map.copyOf(headers);
  }

  /** Retry overrides for one request. */
  public record RetryOptions(int maxRetries, Duration baseDelay, Duration maxDelay) {
    /** Validates retry limits and delays. */
    public RetryOptions {
      if (maxRetries < 0) {
        throw new IllegalArgumentException("maxRetries must not be negative");
      }
      if (baseDelay == null || baseDelay.isNegative() || baseDelay.isZero()) {
        throw new IllegalArgumentException("baseDelay must be positive");
      }
      if (maxDelay == null || maxDelay.compareTo(baseDelay) < 0) {
        throw new IllegalArgumentException("maxDelay must not be less than baseDelay");
      }
    }
  }
}
