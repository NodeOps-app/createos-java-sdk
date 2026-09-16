package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Terminal dimensions in character cells. */
public record PtySize(int rows, @JsonProperty("cols") int columns) {}
