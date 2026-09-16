package sh.createos.model;

/** Filtering and result limits for sandbox listing. */
public record ListSandboxesOptions(int limit, SandboxStatus status) {
  /** Lists all sandboxes without a status filter. */
  public static final ListSandboxesOptions ALL = new ListSandboxesOptions(0, null);

  /** Validates the result limit. */
  public ListSandboxesOptions {
    if (limit < 0) {
      throw new IllegalArgumentException("limit must not be negative");
    }
  }
}
