package network.nodeops.createos.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Dockerfile template build request. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CreateTemplateRequest(String name, String dockerfile, String base) {}
