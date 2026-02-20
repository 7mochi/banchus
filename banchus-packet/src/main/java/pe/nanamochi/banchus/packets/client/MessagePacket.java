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
public class MessagePacket implements ClientPacket {
  private String sender;
  private String content;
  private String target;
  private int senderId;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_MESSAGE;
  }

  @Override
  public void read(IDataReader reader, InputStream stream) throws IOException {
    this.sender = reader.readString(stream);
    this.content = reader.readString(stream);
    this.target = reader.readString(stream);
    this.senderId = reader.readInt32(stream);
  }

  public boolean isDirectMessage() {
    return !this.target.startsWith("#");
  }
}
