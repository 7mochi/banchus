package pe.nanamochi.poc_osu_server.packets.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.poc_osu_server.packets.Packet;
import pe.nanamochi.poc_osu_server.packets.Packets;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveUpdatesPacket implements Packet {
    private int presenceFilter;

    @Override
    public Packets getPacketType() {
        return Packets.OSU_RECEIVE_UPDATES;
    }
}
