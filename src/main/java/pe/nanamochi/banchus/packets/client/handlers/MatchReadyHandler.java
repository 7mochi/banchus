package pe.nanamochi.banchus.packets.client.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.entities.commons.SlotStatus;
import pe.nanamochi.banchus.entities.db.Session;
import pe.nanamochi.banchus.entities.redis.MultiplayerSlot;
import pe.nanamochi.banchus.packets.AbstractPacketHandler;
import pe.nanamochi.banchus.packets.Packets;
import pe.nanamochi.banchus.packets.client.MatchReadyPacket;
import pe.nanamochi.banchus.services.multiplayer.MatchBroadcastService;
import pe.nanamochi.banchus.services.multiplayer.MultiplayerService;

@Component
@RequiredArgsConstructor
public class MatchReadyHandler extends AbstractPacketHandler<MatchReadyPacket> {
  private static final Logger logger = LoggerFactory.getLogger(MatchReadyHandler.class);

  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_MATCH_READY;
  }

  @Override
  public Class<MatchReadyPacket> getPacketClass() {
    return MatchReadyPacket.class;
  }

  @Override
  public void handle(MatchReadyPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    logger.debug("Handling packet: {}", getPacketType());

    if (session.getMultiplayerMatchId() == null) {
      logger.warn(
          "User {} attemped to get ready but they are not in a match.",
          session.getUser().getUsername());
      return;
    }

    MultiplayerSlot slot =
        multiplayerService.findSlotBySessionId(session.getMultiplayerMatchId(), session.getId());
    if (slot == null) {
      logger.warn(
          "User {} attemped to get ready but they don't have a slot.",
          session.getUser().getUsername());
      return;
    }

    if (slot.getStatus() != SlotStatus.NOT_READY.getValue()) {
      logger.warn(
          "User {} attemped to get ready but they are not allowed to.",
          session.getUser().getUsername());
      return;
    }

    slot.setStatus(SlotStatus.READY.getValue());
    multiplayerService.updateSlot(session.getMultiplayerMatchId(), slot);

    matchBroadcastService.broadcastMatchUpdates(session.getMultiplayerMatchId(), true, List.of());
  }
}
