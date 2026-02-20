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
public class MatchStartPacket implements ClientPacket {
  @Override
  public Packets getPacketType() {
    return Packets.OSU_MATCH_START;
  }

  @Override
  public void read(IDataReader reader, InputStream stream) throws IOException {}
}
