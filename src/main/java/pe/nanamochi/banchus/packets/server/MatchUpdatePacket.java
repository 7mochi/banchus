package pe.nanamochi.banchus.packets.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.entities.packets.Match;
import pe.nanamochi.banchus.packets.Packet;
import pe.nanamochi.banchus.packets.Packets;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchUpdatePacket implements Packet {
  private Match match;
  private boolean shouldSendPassword;

  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_MATCH_UPDATE;
  }
}
