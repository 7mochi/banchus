package pe.nanamochi.banchus.packets.server;

import java.io.IOException;
import java.io.OutputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.database.entity.User;
import pe.nanamochi.banchus.io.IDataWriter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPresencePacket implements ServerPacket {
  private int userId;
  private String username;
  private int utcOffset;
  private int countryCode;
  private int permissions;
  private float latitude;
  private float longitude;
  private int globalRank;

  public UserPresencePacket(User user, Session session, int globalRank) {
    this.userId = user.getId();
    this.username = user.getUsername();
    this.utcOffset = session.getUtcOffset();
    this.countryCode = session.getCountry().getId();
    this.permissions = 0; // TODO: permissions
    this.latitude = session.getLatitude();
    this.longitude = session.getLongitude();
    this.globalRank = globalRank;
  }

  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_USER_PRESENCE;
  }

  @Override
  public void write(IDataWriter writer, OutputStream stream) throws IOException {
    writer.writeInt32(stream, userId);
    writer.writeString(stream, username);
    writer.writeUint8(stream, (byte) (utcOffset + 24));
    writer.writeUint8(stream, (byte) countryCode);
    writer.writeUint8(stream, permissions);
    writer.writeFloat32(stream, latitude);
    writer.writeFloat32(stream, longitude);
    writer.writeInt32(stream, globalRank);
  }
}
