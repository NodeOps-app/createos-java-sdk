package network.nodeops.createos.internal;

import com.fasterxml.jackson.databind.JsonNode;

/** Internal JSend response envelope. */
public record JsendEnvelope(String status, JsonNode data, String message, int code) {}
