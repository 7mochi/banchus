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
public class MatchJoinPacket implements ClientPacket {
  private int matchId;
  private String matchPassword;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_MATCH_JOIN;
  }

  @Override
  public void read(IDataReader reader, InputStream stream) throws IOException {
    this.matchId = reader.readInt32(stream);
    this.matchPassword = reader.readString(stream);
  }
}
