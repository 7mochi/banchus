package pe.nanamochi.banchus.packets.client.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.entities.PacketBundle;
import pe.nanamochi.banchus.entities.db.Session;
import pe.nanamochi.banchus.packets.AbstractPacketHandler;
import pe.nanamochi.banchus.packets.PacketWriter;
import pe.nanamochi.banchus.packets.Packets;
import pe.nanamochi.banchus.packets.client.CantSpectatePacket;
import pe.nanamochi.banchus.packets.server.SpectatorCantSpectatePacket;
import pe.nanamochi.banchus.services.*;

@Component
@RequiredArgsConstructor
public class CantSpectateHandler extends AbstractPacketHandler<CantSpectatePacket> {
  private static final Logger logger = LoggerFactory.getLogger(CantSpectateHandler.class);

  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final SpectatorService spectatorService;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_CANT_SPECTATE;
  }

  @Override
  public Class<CantSpectatePacket> getPacketClass() {
    return CantSpectatePacket.class;
  }

  @Override
  public void handle(
      CantSpectatePacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    logger.debug("Handling packet: {}", getPacketType());

    if (session.getSpectatorHostSessionId() == null) {
      logger.warn("A user told us they can't spectate while not spectating anyone.");
      return;
    }

    Session hostSession = sessionService.getSessionByID(session.getSpectatorHostSessionId());

    if (hostSession == null) {
      logger.warn("A user told us they can't spectate anothjer user who is offline.");
      return;
    }

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(stream, new SpectatorCantSpectatePacket(session.getUser().getId()));
    packetBundleService.enqueue(hostSession.getId(), new PacketBundle(stream.toByteArray()));

    for (UUID spectatorSessionId : spectatorService.getMembers(hostSession.getId())) {
      packetBundleService.enqueue(spectatorSessionId, new PacketBundle(stream.toByteArray()));
    }
  }
}
