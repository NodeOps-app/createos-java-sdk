package network.nodeops.createos.model;

/** Pagination offset and maximum result count. */
public record PaginationOptions(int limit, int offset) {
  /** Fetches all available results from the beginning. */
  public static final PaginationOptions ALL = new PaginationOptions(0, 0);

  /** Validates the limit and offset. */
  public PaginationOptions {
    if (limit < 0 || offset < 0) {
      throw new IllegalArgumentException("limit and offset must not be negative");
    }
  }
}
