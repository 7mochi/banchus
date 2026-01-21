package pe.nanamochi.poc_osu_server.packets.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.poc_osu_server.packets.Packet;
import pe.nanamochi.poc_osu_server.packets.Packets;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncePacket implements Packet {
    private String message;

    @Override
    public Packets getPacketType() {
        return Packets.BANCHO_ANNOUNCE;
    }
}
