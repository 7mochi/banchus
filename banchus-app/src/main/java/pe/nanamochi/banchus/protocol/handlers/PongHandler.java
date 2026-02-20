package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.PongPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;

@Component
@HandleClientPacket(value = Packets.OSU_PONG, checkForRestriction = true)
public class PongHandler extends AbstractPacketHandler<PongPacket> {
  @Override
  public void handle(PongPacket packet, Session session, ByteArrayOutputStream responseStream) {}
}
