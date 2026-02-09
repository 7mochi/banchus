package pe.nanamochi.banchus.packets.client.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.entities.commons.SlotStatus;
import pe.nanamochi.banchus.entities.db.Session;
import pe.nanamochi.banchus.entities.redis.MultiplayerSlot;
import pe.nanamochi.banchus.packets.AbstractPacketHandler;
import pe.nanamochi.banchus.packets.PacketWriter;
import pe.nanamochi.banchus.packets.Packets;
import pe.nanamochi.banchus.packets.client.MatchSkipRequestPacket;
import pe.nanamochi.banchus.packets.server.MatchPlayerSkippedPacket;
import pe.nanamochi.banchus.packets.server.MatchSkipPacket;
import pe.nanamochi.banchus.services.MatchBroadcastService;
import pe.nanamochi.banchus.services.MultiplayerService;

@Component
@RequiredArgsConstructor
public class MatchSkipRequestHandler extends AbstractPacketHandler<MatchSkipRequestPacket> {
  private static final Logger logger = LoggerFactory.getLogger(MatchSkipRequestHandler.class);

  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;
  private final PacketWriter packetWriter;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_MATCH_SKIP_REQUEST;
  }

  @Override
  public Class<MatchSkipRequestPacket> getPacketClass() {
    return MatchSkipRequestPacket.class;
  }

  @Override
  public void handle(
      MatchSkipRequestPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    logger.debug("Handling packet: {}", getPacketType());

    if (session.getMultiplayerMatchId() == null) {
      logger.warn(
          "User {} attempted to request a skip but they are not in a match.",
          session.getUser().getUsername());
      return;
    }

    MultiplayerSlot slot =
        multiplayerService.findSlotBySessionId(session.getMultiplayerMatchId(), session.getId());
    if (slot == null) {
      logger.warn(
          "User {} attempted to skip but they don't have a slot.", session.getUser().getUsername());
      return;
    }

    slot.setSkipped(true);
    multiplayerService.updateSlot(session.getMultiplayerMatchId(), slot);

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(stream, new MatchPlayerSkippedPacket(slot.getSlotId()));

    boolean allSkiped = multiplayerService.allSkipped(session.getMultiplayerMatchId());
    if (allSkiped) {
      packetWriter.writePacket(stream, new MatchSkipPacket());
    }

    matchBroadcastService.broadcastToMatch(
        session.getMultiplayerMatchId(), stream.toByteArray(), SlotStatus.PLAYING.getValue());
  }
}
