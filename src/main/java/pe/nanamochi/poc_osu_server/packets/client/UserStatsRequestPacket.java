package pe.nanamochi.poc_osu_server.packets.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.poc_osu_server.packets.Packet;
import pe.nanamochi.poc_osu_server.packets.Packets;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsRequestPacket implements Packet {
    private List<Integer> userIds;

    @Override
    public Packets getPacketType() {
        return Packets.OSU_USER_STATS_REQUEST;
    }
}
