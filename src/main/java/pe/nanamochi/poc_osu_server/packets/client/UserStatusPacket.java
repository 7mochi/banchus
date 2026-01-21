package pe.nanamochi.poc_osu_server.packets.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.poc_osu_server.entities.Mode;
import pe.nanamochi.poc_osu_server.entities.Mods;
import pe.nanamochi.poc_osu_server.entities.Status;
import pe.nanamochi.poc_osu_server.packets.Packet;
import pe.nanamochi.poc_osu_server.packets.Packets;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusPacket implements Packet {
    private Status action;
    private String text;
    private List<Mods> mods;
    private Mode mode;
    private String beatmapChecksum;
    private int beatmapId;

    @Override
    public Packets getPacketType() {
        return Packets.OSU_USER_STATUS;
    }
}
