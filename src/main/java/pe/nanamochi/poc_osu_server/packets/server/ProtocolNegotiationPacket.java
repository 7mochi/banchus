package pe.nanamochi.poc_osu_server.packets.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import pe.nanamochi.poc_osu_server.packets.Packet;
import pe.nanamochi.poc_osu_server.packets.Packets;

@Data
@AllArgsConstructor
public class ProtocolNegotiationPacket implements Packet {
    private int protocolVersion;

    public ProtocolNegotiationPacket() {
        this.protocolVersion = 19;
    }

    @Override
    public Packets getPacketType() {
        return Packets.BANCHO_PROTOCOL_NEOGITIATION;
    }
}
