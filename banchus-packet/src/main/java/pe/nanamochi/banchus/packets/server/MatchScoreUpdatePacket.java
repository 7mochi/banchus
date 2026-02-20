package pe.nanamochi.banchus.packets.server;

import java.io.IOException;
import java.io.OutputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.components.ScoreFrame;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.io.IDataWriter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreUpdatePacket implements ServerPacket {
  private ScoreFrame frame;

  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_MATCH_SCORE_UPDATE;
  }

  @Override
  public void write(IDataWriter writer, OutputStream stream) throws IOException {
    frame.write(writer, stream);
  }
}
