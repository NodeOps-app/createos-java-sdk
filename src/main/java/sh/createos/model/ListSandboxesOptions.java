package sh.createos.model;

import java.util.Map;

/** Filtering and result limits for sandbox listing. */
public record ListSandboxesOptions(int limit, SandboxStatus status, Map<String, String> labels) {
  /** Lists all sandboxes without a status filter. */
  public static final ListSandboxesOptions ALL = new ListSandboxesOptions(0, null, Map.of());

  /** Creates options without a label filter. */
  public ListSandboxesOptions(int limit, SandboxStatus status) {
    this(limit, status, Map.of());
  }

  /** Validates the result limit. */
  public ListSandboxesOptions {
    if (limit < 0) {
      throw new IllegalArgumentException("limit must not be negative");
    }
    labels = labels == null ? Map.of() : Map.copyOf(labels);
  }
}
