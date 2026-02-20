package pe.nanamochi.banchus.packets.server;

import java.io.IOException;
import java.io.OutputStream;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.io.IDataWriter;

@Data
@NoArgsConstructor
public class MatchAllPlayersLoadedPacket implements ServerPacket {
  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_MATCH_ALL_PLAYERS_LOADED;
  }

  @Override
  public void write(IDataWriter writer, OutputStream stream) throws IOException {}
}
