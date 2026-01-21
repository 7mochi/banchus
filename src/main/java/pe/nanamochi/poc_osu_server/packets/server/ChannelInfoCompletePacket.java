package pe.nanamochi.poc_osu_server.packets.server;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.poc_osu_server.packets.Packet;
import pe.nanamochi.poc_osu_server.packets.Packets;

@Data
@NoArgsConstructor
public class ChannelInfoCompletePacket implements Packet {
    @Override
    public Packets getPacketType() {
        return Packets.BANCHO_CHANNEL_INFO_COMPLETE;
    }
}
