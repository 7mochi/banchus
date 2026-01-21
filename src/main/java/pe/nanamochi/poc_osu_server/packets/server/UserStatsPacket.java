package pe.nanamochi.poc_osu_server.packets.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.poc_osu_server.packets.Packet;
import pe.nanamochi.poc_osu_server.packets.Packets;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsPacket implements Packet {
    private int userId;
    private int action;
    private String infoText;
    private String beatmapMd5;
    private int mods;
    private int gamemode;
    private int beatmapId;
    private long rankedScore;
    private float accuracy;
    private int playCount;
    private long totalScore;
    private int globalRank;
    private int performancePoints;

    @Override
    public Packets getPacketType() {
        return Packets.BANCHO_USER_STATS;
    }
}
