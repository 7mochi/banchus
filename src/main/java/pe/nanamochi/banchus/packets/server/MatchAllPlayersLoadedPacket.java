package pe.nanamochi.banchus.packets.server;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.packets.Packet;
import pe.nanamochi.banchus.packets.Packets;

@Data
@NoArgsConstructor
public class MatchAllPlayersLoadedPacket implements Packet {
  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_MATCH_ALL_PLAYERS_LOADED;
  }
}
