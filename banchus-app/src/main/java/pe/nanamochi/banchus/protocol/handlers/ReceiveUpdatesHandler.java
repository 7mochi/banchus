package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.ReceiveUpdatesPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;

@Slf4j
@Component
@HandleClientPacket(value = Packets.OSU_RECEIVE_UPDATES, checkForRestriction = true)
public class ReceiveUpdatesHandler extends AbstractPacketHandler<ReceiveUpdatesPacket> {
  @Override
  public void handle(
      ReceiveUpdatesPacket packet, Session session, ByteArrayOutputStream responseStream) {}
}
