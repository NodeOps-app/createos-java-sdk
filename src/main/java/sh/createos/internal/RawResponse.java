package sh.createos.internal;

import java.io.InputStream;
import java.net.http.HttpHeaders;

/** Streaming response whose body is owned by the caller. */
public record RawResponse(int statusCode, HttpHeaders headers, InputStream body) {}
