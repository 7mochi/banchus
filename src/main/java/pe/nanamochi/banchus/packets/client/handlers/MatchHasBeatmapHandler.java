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
import pe.nanamochi.banchus.packets.PacketWriter;
import pe.nanamochi.banchus.packets.Packets;
import pe.nanamochi.banchus.packets.client.MatchHasBeatmapPacket;
import pe.nanamochi.banchus.services.MatchBroadcastService;
import pe.nanamochi.banchus.services.MultiplayerService;

@Component
@RequiredArgsConstructor
public class MatchHasBeatmapHandler extends AbstractPacketHandler<MatchHasBeatmapPacket> {
  private static final Logger logger = LoggerFactory.getLogger(MatchHasBeatmapHandler.class);

  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;
  private final PacketWriter packetWriter;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_MATCH_HAS_BEATMAP;
  }

  @Override
  public Class<MatchHasBeatmapPacket> getPacketClass() {
    return MatchHasBeatmapPacket.class;
  }

  @Override
  public void handle(
      MatchHasBeatmapPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    logger.debug("Handling packet: {}", getPacketType());

    if (session.getMultiplayerMatchId() == null) {
      logger.warn(
          "User {} attempted to tell us they have the map but they are not in a match.",
          session.getUser().getUsername());
      return;
    }

    MultiplayerSlot slot =
        multiplayerService.findSlotBySessionId(session.getMultiplayerMatchId(), session.getId());
    if (slot == null) {
      logger.warn(
          "User {} attempted to tell us they have the map but they don't have a slot.",
          session.getUser().getUsername());
      return;
    }

    if (slot.getStatus() != SlotStatus.NO_BEATMAP.getValue()) {
      logger.warn(
          "User {} attempted to tell us they have the map but they are not allowed to.",
          session.getUser().getUsername());
      return;
    }

    slot.setStatus(SlotStatus.NOT_READY.getValue());
    multiplayerService.updateSlot(session.getMultiplayerMatchId(), slot);

    matchBroadcastService.broadcastMatchUpdates(session.getMultiplayerMatchId(), true, List.of());
  }
}
