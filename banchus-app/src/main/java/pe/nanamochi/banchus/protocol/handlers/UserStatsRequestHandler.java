package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.UserStatsRequestPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;

@Slf4j
@Component
@HandleClientPacket(value = Packets.OSU_USER_STATS_REQUEST, checkForRestriction = true)
public class UserStatsRequestHandler extends AbstractPacketHandler<UserStatsRequestPacket> {
  @Override
  public void handle(
      UserStatsRequestPacket packet, Session session, ByteArrayOutputStream responseStream) {}
}
