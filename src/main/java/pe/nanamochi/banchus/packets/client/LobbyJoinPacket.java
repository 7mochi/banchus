package pe.nanamochi.banchus.packets.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import pe.nanamochi.banchus.packets.Packet;
import pe.nanamochi.banchus.packets.Packets;

@Data
@AllArgsConstructor
public class LobbyJoinPacket implements Packet {
  @Override
  public Packets getPacketType() {
    return Packets.OSU_LOBBY_JOIN;
  }
}
