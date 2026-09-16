package network.nodeops.createos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import network.nodeops.createos.internal.NdjsonStream;
import network.nodeops.createos.model.CommandStreamEvent;
import network.nodeops.createos.model.CommandStreamFrame;
import org.junit.jupiter.api.Test;

class CommandStreamTest {
  @Test
  void expandsFramesInStableOrder() {
    String frames =
        ": keepalive\n"
            + "data: {\"hb\":true,\"stdout\":\"hello\",\"stderr\":\"warn\",\"exit_code\":0}\n";
    NdjsonStream<CommandStreamFrame> decoder =
        new NdjsonStream<>(
            new ByteArrayInputStream(frames.getBytes(StandardCharsets.UTF_8)),
            new ObjectMapper(),
            CommandStreamFrame.class);

    try (CommandStream stream = new CommandStream(decoder)) {
      assertEquals(CommandStreamEvent.Type.HEARTBEAT, stream.receive().type());
      assertEquals(CommandStreamEvent.Type.STDOUT, stream.receive().type());
      assertEquals(CommandStreamEvent.Type.STDERR, stream.receive().type());
      assertEquals(CommandStreamEvent.Type.EXIT, stream.receive().type());
      assertNull(stream.receive());
    }
  }
}
