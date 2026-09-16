package sh.createos.model;

/** Desktop window position and dimensions. */
public record WindowGeometry(String id, int x, int y, int width, int height, int screen) {}
