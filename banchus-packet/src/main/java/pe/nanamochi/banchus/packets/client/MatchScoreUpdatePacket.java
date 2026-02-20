package pe.nanamochi.banchus.packets.client;

import java.io.IOException;
import java.io.InputStream;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.components.ScoreFrame;
import pe.nanamochi.banchus.core.ClientPacket;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.io.IDataReader;

@Data
@NoArgsConstructor
public class MatchScoreUpdatePacket implements ClientPacket {
  private ScoreFrame frame;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_MATCH_SCORE_UPDATE;
  }

  @Override
  public void read(IDataReader reader, InputStream stream) throws IOException {
    this.frame = ScoreFrame.read(reader, stream);
  }
}
