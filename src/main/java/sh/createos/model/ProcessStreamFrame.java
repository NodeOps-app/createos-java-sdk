package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Raw managed-process connection frame. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessStreamFrame(
    String type,
    @JsonProperty("seq") long sequence,
    String stream,
    @JsonProperty("data_base64") String dataBase64,
    @JsonProperty("exit_code") Integer exitCode,
    String signal,
    @JsonProperty("error") String errorMessage,
    @JsonProperty("oldest_available_seq") long oldestAvailableSequence) {}
