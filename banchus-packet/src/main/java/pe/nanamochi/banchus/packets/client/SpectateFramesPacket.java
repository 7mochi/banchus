package pe.nanamochi.banchus.packets.client;

import java.io.IOException;
import java.io.InputStream;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.components.ReplayFrameBundle;
import pe.nanamochi.banchus.core.ClientPacket;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.io.IDataReader;

@Data
@NoArgsConstructor
public class SpectateFramesPacket implements ClientPacket {
  private ReplayFrameBundle replayFrameBundle;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_SPECTATE_FRAMES;
  }

  @Override
  public void read(IDataReader reader, InputStream stream) throws IOException {
    this.replayFrameBundle = ReplayFrameBundle.read(reader, stream);
  }
}
