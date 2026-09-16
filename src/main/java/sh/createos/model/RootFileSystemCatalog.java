package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Built-in root filesystem catalog. */
public record RootFileSystemCatalog(
    @JsonProperty("rootfs") List<String> rootFileSystems,
    @JsonProperty("default") String defaultValue,
    List<Entry> entries) {
  /** One root filesystem entry. */
  public record Entry(String name, String description, boolean deprecated, String successor) {}
}
