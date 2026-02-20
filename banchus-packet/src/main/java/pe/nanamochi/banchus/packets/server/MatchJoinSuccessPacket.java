package pe.nanamochi.banchus.packets.server;

import java.io.IOException;
import java.io.OutputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.components.Match;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.io.IDataWriter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchJoinSuccessPacket implements ServerPacket {
  private Match match;
  private boolean shouldSendPassword;

  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_MATCH_JOIN_SUCCESS;
  }

  @Override
  public void write(IDataWriter writer, OutputStream stream) throws IOException {
    match.write(writer, stream, shouldSendPassword);
  }
}
