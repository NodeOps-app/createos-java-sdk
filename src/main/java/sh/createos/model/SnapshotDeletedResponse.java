package sh.createos.model;

/** Accepted reusable snapshot deletion. */
public record SnapshotDeletedResponse(String id, SnapshotStatus status) {}
