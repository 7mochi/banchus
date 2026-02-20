package pe.nanamochi.banchus.packets.server;

import java.io.IOException;
import java.io.OutputStream;
import lombok.Data;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.io.IDataWriter;

@Data
public class ProtocolNegotiationPacket implements ServerPacket {
  private int protocolVersion;

  public ProtocolNegotiationPacket() {
    this.protocolVersion = 19;
  }

  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_PROTOCOL_NEOGITIATION;
  }

  @Override
  public void write(IDataWriter writer, OutputStream stream) throws IOException {
    writer.writeInt32(stream, protocolVersion);
  }
}
