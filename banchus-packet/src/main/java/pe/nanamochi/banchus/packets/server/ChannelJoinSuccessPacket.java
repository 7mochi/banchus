package pe.nanamochi.banchus.packets.server;

import java.io.IOException;
import java.io.OutputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.io.IDataWriter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelJoinSuccessPacket implements ServerPacket {
  private String name;

  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_CHANNEL_JOIN_SUCCESS;
  }

  @Override
  public void write(IDataWriter writer, OutputStream stream) throws IOException {
    writer.writeString(stream, name);
  }
}
