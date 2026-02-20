package pe.nanamochi.banchus.packets.client;

import java.io.IOException;
import java.io.InputStream;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.core.ClientPacket;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.io.IDataReader;

@Data
@NoArgsConstructor
public class StartSpectatingPacket implements ClientPacket {
  private int userId;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_START_SPECTATING;
  }

  @Override
  public void read(IDataReader reader, InputStream stream) throws IOException {
    this.userId = reader.readInt32(stream);
  }
}
