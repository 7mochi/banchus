package pe.nanamochi.banchus.packets.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.components.Mode;
import pe.nanamochi.banchus.components.Mods;
import pe.nanamochi.banchus.components.Status;
import pe.nanamochi.banchus.core.ClientPacket;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.io.IDataReader;

@Data
@NoArgsConstructor
public class UserStatusPacket implements ClientPacket {
  private Status action;
  private String text;
  private String beatmapChecksum;
  private List<Mods> mods;
  private Mode mode;
  private int beatmapId;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_USER_STATUS;
  }

  @Override
  public void read(IDataReader reader, InputStream stream) throws IOException {
    this.action = Status.fromValue(reader.readUint8(stream));
    this.text = reader.readString(stream);
    this.beatmapChecksum = reader.readString(stream);
    this.mods = Mods.fromBitmask(reader.readUint32(stream));
    this.mode = Mode.fromValue(reader.readUint8(stream));
    this.beatmapId = reader.readInt32(stream);
  }
}
