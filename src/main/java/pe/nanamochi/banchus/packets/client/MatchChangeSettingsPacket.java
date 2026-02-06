package pe.nanamochi.banchus.packets.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.entities.packets.Match;
import pe.nanamochi.banchus.packets.Packet;
import pe.nanamochi.banchus.packets.Packets;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchChangeSettingsPacket implements Packet {
  private Match match;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_MATCH_CHANGE_SETTINGS;
  }
}
