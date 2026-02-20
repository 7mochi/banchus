package pe.nanamochi.banchus.packets.server;

import java.io.IOException;
import java.io.OutputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.database.entity.Stat;
import pe.nanamochi.banchus.database.entity.User;
import pe.nanamochi.banchus.domain.enums.Mode;
import pe.nanamochi.banchus.io.IDataWriter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsPacket implements ServerPacket {
  private int userId;
  private int action;
  private String infoText;
  private String beatmapMd5;
  private int mods;
  private Mode gamemode;
  private int beatmapId;
  private long rankedScore;
  private float accuracy;
  private int playCount;
  private long totalScore;
  private int globalRank;
  private int performancePoints;

  public UserStatsPacket(User user, Session session, Stat stats, int globalRank) {
    this.userId = user.getId();
    this.action = session.getAction();
    this.infoText = session.getInfoText();
    this.beatmapMd5 = session.getBeatmapMd5();
    this.mods = session.getMods();
    this.gamemode = session.getGamemode();
    this.beatmapId = session.getBeatmapId();
    this.rankedScore = stats.getRankedScore();
    this.accuracy = (float) stats.getAccuracy();
    this.playCount = stats.getPlayCount();
    this.totalScore = stats.getTotalScore();
    this.globalRank = globalRank;
    this.performancePoints = stats.getPerformancePoints();
  }

  @Override
  public Packets getPacketType() {
    return Packets.BANCHO_USER_STATS;
  }

  @Override
  public void write(IDataWriter writer, OutputStream stream) throws IOException {
    writer.writeInt32(stream, userId);
    writer.writeUint8(stream, action);
    writer.writeString(stream, infoText);
    writer.writeString(stream, beatmapMd5);
    writer.writeUint32(stream, mods);
    writer.writeUint8(stream, gamemode.getValue());
    writer.writeInt32(stream, beatmapId);
    writer.writeUint64(stream, rankedScore);
    writer.writeFloat32(stream, accuracy / 100.0f);
    writer.writeUint32(stream, playCount);
    writer.writeUint64(stream, totalScore);
    writer.writeUint32(stream, globalRank);
    writer.writeUint16(stream, performancePoints);
  }
}
