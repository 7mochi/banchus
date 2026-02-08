package pe.nanamochi.banchus.packets.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.entities.packets.ScoreFrame;
import pe.nanamochi.banchus.packets.Packet;
import pe.nanamochi.banchus.packets.Packets;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreUpdatePacket implements Packet {
  private ScoreFrame frame;

  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_MATCH_SCORE_UPDATE;
  }
}
