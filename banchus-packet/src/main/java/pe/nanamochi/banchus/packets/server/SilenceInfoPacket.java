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
public class SilenceInfoPacket implements ServerPacket {
  private int silenceLength;

  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_SILENCE_INFO;
  }

  @Override
  public void write(IDataWriter writer, OutputStream stream) throws IOException {
    writer.writeInt32(stream, silenceLength);
  }
}
