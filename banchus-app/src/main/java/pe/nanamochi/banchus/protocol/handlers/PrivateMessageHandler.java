package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.PrivateMessagePacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;

@Slf4j
@Component
@HandleClientPacket(value = Packets.OSU_PRIVATE_MESSAGE, checkForRestriction = true)
public class PrivateMessageHandler extends AbstractPacketHandler<PrivateMessagePacket> {
  @Override
  public void handle(
      PrivateMessagePacket packet, Session session, ByteArrayOutputStream responseStream) {}
}
