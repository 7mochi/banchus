package pe.nanamochi.poc_osu_server.packets.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.poc_osu_server.packets.Packet;
import pe.nanamochi.poc_osu_server.packets.Packets;

@Data
@NoArgsConstructor
public class StatusUpdateRequestPacket implements Packet {
    @Override
    public Packets getPacketType() {
        return Packets.OSU_STATUS_UPDATE_REQUEST;
    }
}
