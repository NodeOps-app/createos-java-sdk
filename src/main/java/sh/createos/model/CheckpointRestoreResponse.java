package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Identifies the restored checkpoint and its original directory path. */
public record CheckpointRestoreResponse(
    @JsonProperty("checkpoint_id") String checkpointId, String path) {}
