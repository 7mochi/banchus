package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.packets.client.MatchNoBeatmapPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_NO_BEATMAP)
public class MatchNoBeatmapHandler extends AbstractPacketHandler<MatchNoBeatmapPacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  public void handle(
      MatchNoBeatmapPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} attempted to tell us they have no beatmap but they are not in a match.",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findSlotBySessionId(session.getMultiplayerMatchId(), session.getId())
        .ifPresentOrElse(
            slot -> {
              if (slot.getStatus() != SlotStatus.NOT_READY.getValue()) {
                log.warn(
                    "User {} attempted to tell us they have no beatmap but they are not allowed"
                        + " to.",
                    session.getUser().getUsername());
                return;
              }

              slot.setStatus(SlotStatus.NO_BEATMAP.getValue());
              multiplayerService.updateSlot(session.getMultiplayerMatchId(), slot);
              matchBroadcastService.broadcastMatchUpdates(
                  session.getMultiplayerMatchId(), true, List.of());
            },
            () ->
                log.warn(
                    "User {} attempted to tell us they have no beatmap but they don't have a slot.",
                    session.getUser().getUsername()));
  }
}
